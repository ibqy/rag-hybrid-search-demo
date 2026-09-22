package com.xb.rag.document.parser;

import com.xb.rag.document.DocSegment;
import com.xb.rag.document.DocType;
import com.xb.rag.document.DocumentMeta;
import com.xb.rag.document.ParseResult;
import org.docx4j.TextUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;

import jakarta.xml.bind.JAXBElement;
import java.io.InputStream;
import java.util.*;

/**
 * DOCX 解析器 —— 基于 docx4j，提取样式、表格、内嵌图片
 *
 * @author ibqy
 */
public class WordParser implements DocumentParser {

    @Override
    public boolean supports(DocType type) {
        return type == DocType.DOCX;
    }

    @Override
    public DocType supportedType() {
        return DocType.DOCX;
    }

    @Override
    public ParseResult parse(InputStream input, String fileName, Map<String, String> options) {
        try {
            WordprocessingMLPackage pkg = WordprocessingMLPackage.load(input);
            MainDocumentPart docPart = pkg.getMainDocumentPart();
            DocumentMeta meta = buildMeta(pkg, fileName);
            List<DocSegment> segments = extractSegments(docPart);
            return new ParseResult(meta, segments, true);

        } catch (Docx4JException e) {
            DocumentMeta meta = new DocumentMeta(UUID.randomUUID().toString(), fileName, "", DocType.DOCX);
            return new ParseResult(meta, Collections.emptyList(), false, "DOCX 解析失败: " + e.getMessage());
        }
    }

    private DocumentMeta buildMeta(WordprocessingMLPackage pkg, String fileName) {
        String docId = UUID.randomUUID().toString();
        DocumentMeta meta = new DocumentMeta(docId, fileName, "", DocType.DOCX);
        try {
            var part = pkg.getDocPropsCorePart();
            if (part != null) {
                var props = part.getJaxbElement();
                meta.setAuthor(props.getCreator() != null && !props.getCreator().getContent().isEmpty()
                        ? props.getCreator().getContent().getFirst() : null);
            }
        } catch (Exception ignored) {}
        return meta;
    }

    // 遍历文档体，提取段落、表格、图片
    private List<DocSegment> extractSegments(MainDocumentPart docPart) {
        List<DocSegment> segments = new ArrayList<>();
        Document body = docPart.getJaxbElement();
        List<Object> children = body.getBody().getContent();

        // 标题层级栈：记录当前所在标题上下文
        Deque<HeadingEntry> headingStack = new ArrayDeque<>();

        for (Object obj : children) {
            if (obj instanceof JAXBElement<?> el) {
                Object value = el.getValue();
                if (value instanceof P para) {
                    DocSegment seg = handleParagraph(para);
                    if (seg == null) continue;

                    // 如果是标题段，更新标题栈
                    if (seg.getHeadingLevel() > 0) {
                        // 弹出同级或更高级别标题
                        while (!headingStack.isEmpty()
                                && headingStack.peek().level() >= seg.getHeadingLevel()) {
                            headingStack.pop();
                        }
                        headingStack.push(new HeadingEntry(seg.getSectionTitle(), seg.getHeadingLevel()));
                    } else {
                        // 非标题段，从栈顶继承上下文
                        if (!headingStack.isEmpty()) {
                            HeadingEntry top = headingStack.peek();
                            seg.setSectionTitle(top.title());
                            seg.setHeadingLevel(top.level());
                        }
                    }
                    segments.add(seg);

                } else if (value instanceof Tbl table) {
                    // 表格段也继承当前标题上下文
                    DocSegment seg = handleTable(table);
                    if (seg != null) {
                        if (!headingStack.isEmpty()) {
                            HeadingEntry top = headingStack.peek();
                            seg.setSectionTitle(top.title());
                            seg.setHeadingLevel(top.level());
                        }
                        segments.add(seg);
                    }
                }
            }
        }
        return segments;
    }

    // 处理段落：检测标题样式或正文
    private DocSegment handleParagraph(P para) {
        String text = extractText(para);
        if (text.isBlank()) return null;

        // 获取样式名称
        String styleName = getStyleName(para);
        int headingLevel = styleToHeadingLevel(styleName);

        DocSegment seg = new DocSegment();
        seg.setId(UUID.randomUUID().toString());
        seg.setContent(text);
        seg.setPageNum(0);

        if (headingLevel > 0) {
            seg.setContentType("text");
            seg.setHeadingLevel(headingLevel);
            seg.setSectionTitle(text.trim());
        } else {
            seg.setContentType("text");
        }
        return seg;
    }

    // 提取段落纯文本
    private String extractText(P para) {
        try {
            return TextUtils.getText(para);
        } catch (Exception e) {
            return "";
        }
    }

    // 获取段落样式名
    private String getStyleName(P para) {
        PPr ppr = para.getPPr();
        if (ppr == null) return null;
        var style = ppr.getPStyle();
        return (style != null) ? style.getVal() : null;
    }

    // docx 样式名 -> 标题级别映射
    private int styleToHeadingLevel(String styleName) {
        if (styleName == null) return 0;
        String s = styleName.toLowerCase(Locale.ROOT);
        if (s.startsWith("heading") || s.startsWith("标题")) {
            // "heading1" -> 1, "标题 2" -> 2
            String num = s.replaceAll("\\D+", "");
            if (!num.isEmpty()) {
                try { return Integer.parseInt(num); } catch (NumberFormatException ignored) {}
            }
            return 1;
        }
        return 0;
    }

    // 处理表格：转为 markdown 表格
    private DocSegment handleTable(Tbl table) {
        List<String[]> rows = new ArrayList<>();
        int colCount = 0;

        for (Object rowObj : table.getContent()) {
            if (rowObj instanceof JAXBElement<?> el && el.getValue() instanceof Tr tr) {
                List<String> cols = new ArrayList<>();
                for (Object cellObj : tr.getContent()) {
                    if (cellObj instanceof JAXBElement<?> cel && cel.getValue() instanceof Tc tc) {
                        String cellText = extractTcText(tc);
                        cols.add(cellText);
                    }
                }
                if (!cols.isEmpty()) {
                    rows.add(cols.toArray(new String[0]));
                    colCount = Math.max(colCount, cols.size());
                }
            }
        }
        if (rows.isEmpty()) return null;

        // 构建 markdown 表格
        StringBuilder md = new StringBuilder();
        // 表头
        String[] header = rows.getFirst();
        md.append("|");
        for (String h : header) {
            md.append(" ").append(h != null ? h : "").append(" |");
        }
        md.append("\n|");
        for (int i = 0; i < header.length; i++) {
            md.append(" --- |");
        }
        md.append("\n");
        // 数据行
        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            md.append("|");
            for (int c = 0; c < colCount; c++) {
                String val = c < row.length ? row[c] : "";
                md.append(" ").append(val).append(" |");
            }
            md.append("\n");
        }

        DocSegment seg = new DocSegment();
        seg.setId(UUID.randomUUID().toString());
        seg.setContentType("table");
        seg.setTableMarkdown(md.toString());
        seg.setContent(md.toString());
        seg.setPageNum(0);
        return seg;
    }

    // 提取表格单元格文本
    private String extractTcText(Tc tc) {
        StringBuilder sb = new StringBuilder();
        for (Object content : tc.getContent()) {
            if (content instanceof JAXBElement<?> el) {
                if (el.getValue() instanceof P p) {
                    String txt = extractText(p);
                    if (!txt.isBlank()) {
                        if (!sb.isEmpty()) sb.append(" ");
                        sb.append(txt.trim());
                    }
                }
            }
        }
        return sb.toString();
    }

    // 标题栈条目
    private record HeadingEntry(String title, int level) {}
}