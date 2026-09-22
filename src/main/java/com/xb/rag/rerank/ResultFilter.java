package com.xb.rag.rerank;

import com.xb.rag.retrieval.SearchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 结果过滤器 —— 阈值过滤、去重、令牌预算截断等静态工具方法
 *
 * @author ibqy
 */
public final class ResultFilter {

    private ResultFilter() {}

    /**
     * 根据分数阈值过滤 —— 移除低于 threshold 的结果
     */
    public static List<SearchResult> filterByThreshold(List<SearchResult> results, double threshold) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();
    }

    /**
     * 内容去重 —— 移除文本重叠度超过 90% 的块，保留分数较高的
     */
    public static List<SearchResult> deduplicate(List<SearchResult> results) {
        if (results == null || results.size() <= 1) {
            return results == null ? List.of() : new ArrayList<>(results);
        }

        // 按分数降序排列，优先保留高分项
        List<SearchResult> sorted = results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();

        List<SearchResult> unique = new ArrayList<>();
        for (SearchResult current : sorted) {
            boolean isDuplicate = false;
            for (SearchResult kept : unique) {
                if (textOverlapRatio(current.getContent(), kept.getContent()) > 0.9) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                unique.add(current);
            }
        }
        return unique;
    }

    /**
     * 计算两段文本的字符级重叠比例（基于较短文本）
     */
    private static double textOverlapRatio(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        String shorter = a.length() <= b.length() ? a : b;
        String longer  = a.length() <= b.length() ? b : a;
        int matchLen = 0;
        // 滑动窗口法：检查较短文本中的子串在较长文本中出现的比例
        int step = Math.max(1, shorter.length() / 100); // 采样步长，避免 O(n^2)
        for (int i = 0; i <= shorter.length() - step; i += step) {
            int end = Math.min(i + step, shorter.length());
            String sub = shorter.substring(i, end);
            if (longer.contains(sub)) {
                matchLen += sub.length();
            }
        }
        return (double) matchLen / shorter.length();
    }

    /**
     * 根据令牌预算截断 —— 粗略估计的 token 数 (charCount / 4)，超出时移除尾部低分项
     */
    public static List<SearchResult> limitTokenCount(List<SearchResult> results, int maxTokens) {
        if (results == null || results.isEmpty() || maxTokens <= 0) {
            return List.of();
        }

        List<SearchResult> sorted = results.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();

        List<SearchResult> withinBudget = new ArrayList<>();
        int totalTokens = 0;
        for (SearchResult r : sorted) {
            int tokens = estimateTokens(r.getContent());
            if (totalTokens + tokens > maxTokens) {
                break;
            }
            withinBudget.add(r);
            totalTokens += tokens;
        }
        return withinBudget;
    }

    /**
     * 粗略估算文本的令牌数：中文字符 / 1.5 + 英文单词数
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int chineseChars = 0;
        int asciiWords = 0;
        boolean inWord = false;
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chineseChars++;
            } else if (Character.isLetter(c)) {
                if (!inWord) {
                    asciiWords++;
                    inWord = true;
                }
            } else {
                inWord = false;
            }
        }
        // 中文约 1.5 字符 / token，英文约 1 token / 单词
        return (int) (chineseChars / 1.5) + asciiWords + (text.length() / 10);
    }

    /**
     * 综合过滤：阈值 -> 去重 -> 令牌截断，一步完成
     */
    public static List<SearchResult> combineAndFilter(
            List<SearchResult> results, double threshold, int maxTokens) {
        List<SearchResult> filtered = filterByThreshold(results, threshold);
        List<SearchResult> deduped = deduplicate(filtered);
        return limitTokenCount(deduped, maxTokens);
    }
}