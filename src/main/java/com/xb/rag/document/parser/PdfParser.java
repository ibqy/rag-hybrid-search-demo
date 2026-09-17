package com.xb.rag.document.parser;

import com.xb.rag.document.DocSegment;
import com.xb.rag.document.DocType;
import com.xb.rag.document.DocumentMeta;
import com.xb.rag.document.ParseResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PDF 解析器 —— 基于 PDFBox，逐页提取 + 页眉页脚过滤 + 简单表格检测
 */
public class PdfParser implements DocumentParser {

    // 页眉页脚 Y 坐标阈值（相对页面高度百分比）
    private static final double HEADER_RATIO = 0.08;
    private static final double FOOTER_RATIO = 0.92;

    @Override
    public boolean supports(DocType type) {
        return type == DocType.PDF;
    }

    @Override
    public DocType supportedType() {
        return DocType.PDF;
    }

    @Override
    public ParseResult parse(InputStream input, String fileName, Map<String, String> options) {
        try (var buffer = new org.apache.pdfbox.io.RandomAccessReadBuffer(input);
             PDDocument doc = org.apache.pdfbox.Loader.loadPDF(buffer)) {
            DocumentMeta meta = buildMeta(doc, fileName);
            List<DocSegment> segments = new ArrayList<>();
            int totalPages = doc.getNumberOfPages();

            for (int i = 0; i < totalPages; i++) {
                PDPage page = doc.getPage(i);
                List<DocSegment> pageSegments = extractPageSegments(doc, page, i, totalPages);
                segments.addAll(pageSegments);
            }
            meta.setPageCount(totalPages);
            return new ParseResult(meta, segments, true);

        } catch (IOException e) {
            DocumentMeta meta = new DocumentMeta(UUID.randomUUID().toString(), fileName, "", DocType.PDF);
            return new ParseResult(meta, Collections.emptyList(), false, "PDF 解析失败: " + e.getMessage());
        }
    }

    // 构建基础元信息
    private DocumentMeta buildMeta(PDDocument doc, String fileName) {
        String docId = UUID.randomUUID().toString();
        DocumentMeta meta = new DocumentMeta(docId, fileName, "", DocType.PDF);
        try {
            var info = doc.getDocumentInformation();
            if (info != null) {
                meta.setAuthor(info.getAuthor());
                meta.setCreatedAt(Optional.ofNullable(info.getCreationDate()).map(date -> date.toInstant().toString()).orElse(null));
            }
        } catch (Exception ignored) {}
        return meta;
    }

    // 提取单页内容，返回该页的切片列表
    private List<DocSegment> extractPageSegments(PDDocument doc, PDPage page, int pageIndex, int totalPages) {
        List<DocSegment> segments = new ArrayList<>();
        PDRectangle mediaBox = page.getMediaBox();
        float pageHeight = mediaBox.getHeight();
        float headerY = (float) (pageHeight * HEADER_RATIO);
        float footerY = (float) (pageHeight * FOOTER_RATIO);

        try {
            // 用 Y 坐标敏感的 stripper 提取文本
            CoordStripper stripper = new CoordStripper(headerY, footerY);
            stripper.setStartPage(pageIndex + 1);
            stripper.setEndPage(pageIndex + 1);
            stripper.setSortByPosition(true);
            String pageText = stripper.getText(doc);

            if (pageText.isBlank()) return segments;

            // 按空行分段，每段作为一个 segment
            String[] blocks = pageText.split("\\n{2,}");
            int segIdx = 0;
            for (String block : blocks) {
                block = block.trim();
                if (block.isEmpty()) continue;

                // 简单表格检测：包含连续竖线或含明显多列的数据行
                boolean isTable = detectTable(block);

                DocSegment seg = new DocSegment();
                seg.setId(UUID.randomUUID().toString());
                seg.setDocId("");  // 后续由 processor 填充
                seg.setPageNum(pageIndex + 1);

                if (isTable) {
                    seg.setContentType("table");
                    seg.setTableMarkdown(convertToMarkdownTable(block));
                    seg.setContent(block);
                } else {
                    seg.setContentType("text");
                    seg.setContent(block);
                    // 尝试从首行检测标题级别
                    int heading = detectHeadingLevel(block);
                    if (heading > 0) {
                        seg.setHeadingLevel(heading);
                        seg.setSectionTitle(block.lines().findFirst().orElse("").replaceAll("#+\\s*", "").trim());
                    }
                }
                segments.add(seg);
                segIdx++;
            }
        } catch (IOException e) {
            DocSegment errSeg = new DocSegment(UUID.randomUUID().toString(), "", "解析异常: " + e.getMessage(), "text");
            errSeg.setPageNum(pageIndex + 1);
            segments.add(errSeg);
        }
        return segments;
    }

    // 简单表格启发式检测：行含多个 tab/空格分隔的列，且多行对齐
    private boolean detectTable(String block) {
        String[] lines = block.split("\n");
        if (lines.length < 2) return false;
        long dataLines = Arrays.stream(lines)
                .filter(l -> l.trim().split("[ \\t]{2,}").length >= 3)
                .count();
        return dataLines >= 2;
    }

    // 转 markdown 表格（简单空格切分）
    private String convertToMarkdownTable(String block) {
        String[] lines = block.split("\n");
        StringBuilder md = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String[] cols = lines[i].trim().split("[ \\t]{2,}");
            md.append("| ");
            for (String c : cols) {
                md.append(c.trim()).append(" | ");
            }
            md.append("\n");
            if (i == 0) {
                md.append("|");
                for (String c : cols) {
                    md.append(" --- |");
                }
                md.append("\n");
            }
        }
        return md.toString();
    }

    // 从行首 # 或大号字体推测标题级别
    private int detectHeadingLevel(String text) {
        String firstLine = text.lines().findFirst().orElse("").trim();
        if (firstLine.startsWith("#### ")) return 4;
        if (firstLine.startsWith("### ")) return 3;
        if (firstLine.startsWith("## ")) return 2;
        if (firstLine.startsWith("# ")) return 1;
        return 0;
    }

    // ---------- 自定义 TextStripper：按 Y 坐标过滤页眉页脚 ----------

    private static class CoordStripper extends PDFTextStripper {

        private final float headerY;
        private final float footerY;

        protected CoordStripper(float headerY, float footerY) throws IOException {
            super();
            this.headerY = headerY;
            this.footerY = footerY;
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            float y = text.getY();
            // 跳过页眉/页脚区域的文本
            if (y < headerY || y > footerY) return;
            super.processTextPosition(text);
        }
    }
}