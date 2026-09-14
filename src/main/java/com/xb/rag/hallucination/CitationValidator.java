package com.xb.rag.hallucination;

import com.xb.rag.retrieval.SearchResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 引用验证器 —— 检查 LLM 回答中的引用标记 [N] 是否为有效的引用
 */
public final class CitationValidator {

    /** 匹配 [数字] 格式的引用标记 */
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)]");

    private CitationValidator() {}

    /**
     * 验证回答中所有 [N] 引用是否在有效来源范围内
     *
     * @param answer   LLM 回答文本
     * @param sources  有效来源列表
     * @return 无效引用编号列表（空列表表示全部有效）
     */
    public static List<Integer> validateCitations(String answer, List<SearchResult> sources) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        List<Integer> cited = extractCitations(answer);
        if (cited.isEmpty()) {
            return List.of();
        }
        // 来源索引：1-based
        Set<Integer> validIndexes = new HashSet<>();
        for (int i = 0; i < sources.size(); i++) {
            validIndexes.add(i + 1);
        }

        List<Integer> invalid = new ArrayList<>();
        for (int idx : cited) {
            if (!validIndexes.contains(idx)) {
                invalid.add(idx);
            }
        }
        return invalid;
    }

    /**
     * 从回答中提取所有引用编号 [N]
     */
    public static List<Integer> extractCitations(String answer) {
        if (answer == null || answer.isBlank()) {
            return List.of();
        }
        List<Integer> citations = new ArrayList<>();
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            citations.add(Integer.parseInt(matcher.group(1)));
        }
        return citations;
    }

    /**
     * 构建拒绝回答的响应消息
     */
    public static String buildRejectionResponse(String question) {
        return PromptConstants.REJECTION_MESSAGE;
    }

    /**
     * 构建带严格约束的系统提示词
     * 包含完整上下文
     */
    public static String buildPromptWithConstraints(String question, String context) {
        return PromptConstants.SYSTEM_PROMPT + "\n\n"
                + "以下是参考资料：\n"
                + context + "\n\n"
                + "问题：" + question;
    }
}