package com.xb.rag.context;

import com.xb.rag.retrieval.SearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 上下文构建器 —— 将检索结果组装为可供 LLM 直接使用的引用格式
 */
public class ContextBuilder {

    private static final int DEFAULT_MAX_TOKENS = 4096;

    /**
     * 构建基础引用上下文（不含元数据）
     * 格式：[1] 内容... \n [2] 内容...
     */
    public static String buildContext(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        // 按分数降序排列
        List<SearchResult> sorted = sortByRank(results);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            SearchResult r = sorted.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append(r.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建带元数据的引用上下文
     * 格式：[1] (来源: 文档名, 页码: X) \n 内容...
     */
    public static String buildContextWithMeta(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        List<SearchResult> sorted = sortByRank(results);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            SearchResult r = sorted.get(i);
            String metaStr = formatMeta(r);
            sb.append("[").append(i + 1).append("] ")
                    .append(metaStr).append("\n")
                    .append(r.getContent()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 若上下文超过 maxTokens，从最低分项开始移除
     */
    public static String truncateIfExceeds(String context, int maxTokens) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        if (maxTokens <= 0) {
            return "";
        }
        // 先按换行拆分为引用块
        String[] blocks = context.split("\n");
        // 逆向计算：从末尾开始累加，超出则截断
        List<String> kept = new ArrayList<>();
        int totalTokens = 0;
        for (int i = blocks.length - 1; i >= 0; i--) {
            String block = blocks[i];
            if (block.isBlank()) continue;
            int t = estimateTokens(block);
            if (totalTokens + t > maxTokens) {
                break;
            }
            kept.add(0, block);
            totalTokens += t;
        }
        // 若全超了但至少保留一条
        if (kept.isEmpty() && blocks.length > 0) {
            kept.add(blocks[0]);
        }
        return String.join("\n", kept);
    }

    /**
     * 构建带引用元数据的上下文并做截断
     */
    public static String buildContextWithMetaAndTruncate(
            List<SearchResult> results, int maxTokens) {
        String ctx = buildContextWithMeta(results);
        return truncateIfExceeds(ctx, maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS);
    }

    /**
     * 构建基础上下文并做截断
     */
    public static String buildContextAndTruncate(
            List<SearchResult> results, int maxTokens) {
        String ctx = buildContext(results);
        return truncateIfExceeds(ctx, maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS);
    }

    // ========== 内部辅助 ==========

    private static List<SearchResult> sortByRank(List<SearchResult> results) {
        return results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();
    }

    private static String formatMeta(SearchResult r) {
        String docName = r.getDocName() != null ? r.getDocName() : "未知文档";
        int pageNum = r.getPageNum();
        return "(来源: " + docName + ", 页码: " + pageNum + ")";
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chinese = 0, words = 0;
        boolean inWord = false;
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chinese++;
            } else if (Character.isLetter(c)) {
                if (!inWord) { words++; inWord = true; }
            } else { inWord = false; }
        }
        return (int) (chinese / 1.5) + words + (text.length() / 10);
    }

    /**
     * 搜索结果引用 —— 包含序号、原始结果和格式化后的引用串
     */
    public static class SearchResultCitation {
        private final int id;
        private final SearchResult result;
        private final String citationStr;

        public SearchResultCitation(int id, SearchResult result, String citationStr) {
            this.id = id;
            this.result = result;
            this.citationStr = citationStr;
        }

        public int getId() { return id; }
        public SearchResult getResult() { return result; }
        public String getCitationStr() { return citationStr; }
    }

    /**
     * 构建 SearchResultCitation 列表
     */
    public static List<SearchResultCitation> buildCitations(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<SearchResult> sorted = sortByRank(results);
        List<SearchResultCitation> citations = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            SearchResult r = sorted.get(i);
            String metaStr = formatMeta(r);
            String citationStr = "[" + (i + 1) + "] " + metaStr + "\n" + r.getContent();
            citations.add(new SearchResultCitation(i + 1, r, citationStr));
        }
        return citations;
    }
}