package com.xb.rag.document.cleaner;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文档文本清洗工具：去页眉页脚、标准化空白、去重、去目录
 */
public class DocCleaner {

    // 常见页眉页脚模式：页码、版权声明、"Page X of Y" 等
    private static final Pattern HEADER_FOOTER_PATTERN = Pattern.compile(
            "(?m)^(\\d+\\s*[-–]\\s*\\d+|第\\s*\\d+\\s*页|Page\\s+\\d+\\s+of\\s+\\d+|©|Copyright|版权所有).*$",
            Pattern.CASE_INSENSITIVE
    );

    // 全角字符转半角
    private static final Pattern FULLWIDTH = Pattern.compile("[\\uFF01-\\uFF5E]");
    private static final Pattern FULLWIDTH_SPACE = Pattern.compile("　");

    // 连续空行/空白压缩
    private static final Pattern MULTI_BLANK = Pattern.compile("\\n{3,}");
    private static final Pattern MULTI_SPACE = Pattern.compile("[ \\t]{2,}");

    // 目录行特征："第一章..."、"第1节..."、"1.2.3..."、"|  ..." 等
    private static final Pattern TOC_LINE = Pattern.compile("^\\s*([第\\d一二三四五六七八九十百千]+[章节篇部]|\\d{1,2}\\.\\d{1,2}\\.?\\d{0,2}|•)\\s.*\\.{2,}\\s*\\d+\\s*$");

    // 目录段标记
    private static final Pattern TOC_START = Pattern.compile("(?m)^\\s*目[录録]|^\\s*CONTENTS|^\\s*Table\\s+of\\s+Contents",
            Pattern.CASE_INSENSITIVE);

    /**
     * 去除页眉页脚的行
     */
    public static String removeHeadersFooters(String text) {
        return HEADER_FOOTER_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * 标准化空白：全角→半角，去掉特殊字符，压缩换行
     */
    public static String normalizeWhitespace(String text) {
        if (text == null) return "";
        String s = FULLWIDTH_SPACE.matcher(text).replaceAll(" ");
        s = FULLWIDTH.matcher(s).matcher().replaceAll(m -> {
            char ch = (char) (m.group().charAt(0) - 0xFEE0);
            return String.valueOf(ch);
        });
        // 去掉控制字符（保留换行回车）
        s = s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        s = MULTI_BLANK.matcher(s).replaceAll("\n\n");
        s = MULTI_SPACE.matcher(s).replaceAll(" ");
        return s.trim();
    }

    /**
     * 去除重复段落（连续相同文本只保留一个）
     */
    public static String removeDuplicates(String text) {
        if (text == null) return "";
        String[] lines = text.split("\n");
        Set<String> seen = new LinkedHashSet<>();
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                sb.append(line).append("\n");
                continue;
            }
            if (seen.add(trimmed)) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 去除目录章节
     */
    public static String removeToc(String text) {
        if (text == null) return "";
        String[] lines = text.split("\n");
        boolean inToc = false;
        boolean tocFound = false;
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (!tocFound && TOC_START.matcher(line).find()) {
                inToc = true;
                tocFound = true;
                continue; // 跳过目录标题行
            }
            if (inToc) {
                // 目录行特征匹配或空白行
                if (line.trim().isEmpty() || TOC_LINE.matcher(line).matches()) {
                    continue;
                }
                // 遇到非目录行结束目录区
                inToc = false;
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * 执行全部清洗流程
     */
    public static String clean(String text) {
        if (text == null) return "";
        String s = removeHeadersFooters(text);
        s = removeToc(s);
        s = removeDuplicates(s);
        s = normalizeWhitespace(s);
        return s;
    }
}