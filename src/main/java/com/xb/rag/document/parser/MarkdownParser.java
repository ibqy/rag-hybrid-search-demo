package com.xb.rag.document.parser;

import com.xb.rag.document.DocSegment;
import com.xb.rag.document.DocType;
import com.xb.rag.document.DocumentMeta;
import com.xb.rag.document.ParseResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 解析器 —— 基于正则 + JSoup 辅助，解析标题树、代码块
 */
public class MarkdownParser implements DocumentParser {

    // 标题行
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    // 代码块（fenced）
    private static final Pattern CODE_BLOCK = Pattern.compile("```(\\w*)\\n([\\s\\S]*?)```", Pattern.MULTILINE);

    @Override
    public boolean supports(DocType type) {
        return type == DocType.MARKDOWN;
    }

    @Override
    public DocType supportedType() {
        return DocType.MARKDOWN;
    }

    @Override
    public ParseResult parse(InputStream input, String fileName, Map<String, String> options) {
        try {
            String content = readAll(input);
            DocumentMeta meta = buildMeta(fileName);
            List<DocSegment> segments = extractSegments(content);
            return new ParseResult(meta, segments, true);

        } catch (IOException e) {
            DocumentMeta meta = new DocumentMeta(UUID.randomUUID().toString(), fileName, "", DocType.MARKDOWN);
            return new ParseResult(meta, Collections.emptyList(), false, "Markdown 解析失败: " + e.getMessage());
        }
    }

    private DocumentMeta buildMeta(String fileName) {
        String docId = UUID.randomUUID().toString();
        return new DocumentMeta(docId, fileName, "", DocType.MARKDOWN);
    }

    private String readAll(InputStream input) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    // 将 markdown 拆分为多个 DocSegment
    private List<DocSegment> extractSegments(String content) {
        List<DocSegment> segments = new ArrayList<>();

        // 1. 提取所有代码块，原地替换为占位符，防止内部 # 被当标题
        Map<String, String> codePlaceholders = new LinkedHashMap<>();
        StringBuffer sb = new StringBuffer();
        Matcher codeMatcher = CODE_BLOCK.matcher(content);
        int idx = 0;
        while (codeMatcher.find()) {
            String lang = codeMatcher.group(1);
            String code = codeMatcher.group(2);
            String placeholder = "%%CODE_BLOCK_" + (idx++) + "%%";
            codePlaceholders.put(placeholder, code);

            DocSegment seg = new DocSegment();
            seg.setId(UUID.randomUUID().toString());
            seg.setContentType("code");
            seg.setContent(code);
            seg.getMetadata().put("language", lang.isBlank() ? "plaintext" : lang);
            segments.add(seg);

            codeMatcher.appendReplacement(sb, placeholder);
        }
        codeMatcher.appendTail(sb);
        String textWithoutCode = sb.toString();

        // 2. 按标题行拆分段
        Matcher headingMatcher = HEADING.matcher(textWithoutCode);
        int lastStart = 0;
        String lastSectionTitle = "";
        int lastHeadingLevel = 0;

        while (headingMatcher.find()) {
            // 将上一个标题和文本内容作为一个 segment（跳过第一个匹配前的部分）
            if (headingMatcher.start() > 0 && lastHeadingLevel > 0) {
                String sectionContent = textWithoutCode.substring(lastStart, headingMatcher.start()).trim();
                if (!sectionContent.isEmpty()) {
                    // 重新放入代码块原文
                    sectionContent = restoreCodeBlocks(sectionContent, codePlaceholders);
                    DocSegment seg = buildTextSegment(sectionContent, lastSectionTitle, lastHeadingLevel);
                    if (seg != null) segments.add(seg);
                }
            }

            // 只有当这是第一个标题时才更新起始位置
            if (lastHeadingLevel == 0) {
                lastStart = headingMatcher.start();
            }
            lastStart = headingMatcher.start();
            lastHeadingLevel = headingMatcher.group(1).length();
            lastSectionTitle = headingMatcher.group(2).trim();
        }

        // 最后一个标题后的剩余内容
        if (lastHeadingLevel > 0) {
            String tail = textWithoutCode.substring(lastStart).trim();
            // 去掉标题行本身
            tail = tail.replaceAll("^#{1,6}\\s+.*", "").trim();
            if (!tail.isEmpty()) {
                tail = restoreCodeBlocks(tail, codePlaceholders);
                DocSegment seg = buildTextSegment(tail, lastSectionTitle, lastHeadingLevel);
                if (seg != null) segments.add(seg);
            }
        } else {
            // 无标题的情况，整篇作为一个段
            String all = restoreCodeBlocks(textWithoutCode.trim(), codePlaceholders);
            if (!all.isEmpty()) {
                DocSegment seg = new DocSegment();
                seg.setId(UUID.randomUUID().toString());
                seg.setContentType("text");
                seg.setContent(all);
                seg.setHeadingLevel(0);
                segments.add(seg);
            }
        }

        return segments;
    }

    // 构建文本段
    private DocSegment buildTextSegment(String content, String sectionTitle, int headingLevel) {
        if (content.isBlank()) return null;
        DocSegment seg = new DocSegment();
        seg.setId(UUID.randomUUID().toString());
        seg.setContentType("text");
        seg.setContent(content);
        seg.setSectionTitle(sectionTitle);
        seg.setHeadingLevel(headingLevel);
        return seg;
    }

    // 将占位符还原为代码块原文
    private String restoreCodeBlocks(String text, Map<String, String> placeholders) {
        String result = text;
        for (var entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}