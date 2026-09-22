package com.xb.rag.hallucination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 幻觉检测服务 —— 利用 LLM 自我验证回答是否完全基于提供的参考资料
 *
 * @author ibqy
 */
@Service
public class HallucinationDetector {

    private static final Logger log = LoggerFactory.getLogger(HallucinationDetector.class);

    private final ChatModel chatModel;

    public HallucinationDetector(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 检查回答是否存在幻觉
     *
     * @param question 用户原始问题
     * @param context  检索到的参考资料上下文
     * @param answer   LLM 生成的回答
     * @return 幻觉检查结果
     */
    public HallucinationCheckResult check(String question, String context, String answer) {
        if (context == null || context.isBlank()) {
            return new HallucinationCheckResult(true,
                    List.of("参考资料为空，无法验证"), 1.0);
        }
        if (answer == null || answer.isBlank()) {
            return new HallucinationCheckResult(false, List.of(), 0.0);
        }

        // 构建验证提示词
        PromptTemplate template = new PromptTemplate(PromptConstants.VERIFICATION_PROMPT);
        Prompt prompt = template.create(Map.of("context", context, "answer", answer));

        try {
            ChatResponse response = chatModel.call(prompt);
            String llmOutput = response.getResult().getOutput().getText();
            return parseVerificationResult(llmOutput);
        } catch (Exception e) {
            log.error("调用 LLM 进行幻觉检测失败: {}", e.getMessage());
            // 降级：检测失败时默认标记为疑似幻觉，避免错误信息传播
            return new HallucinationCheckResult(true,
                    List.of("幻觉检测服务异常，暂时无法验证"), 0.5);
        }
    }

    /**
     * 解析 LLM 返回的验证结果
     */
    private HallucinationCheckResult parseVerificationResult(String output) {
        if (output == null || output.isBlank()) {
            return new HallucinationCheckResult(true, List.of("无法解析验证结果"), 0.5);
        }

        boolean hasHallucination = false;
        List<String> details = new ArrayList<>();
        double confidence = 0.5;

        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();

            // 解析"幻觉判定"
            if (trimmed.contains("幻觉判定") || trimmed.contains("判定")) {
                hasHallucination = trimmed.contains("是");
            }

            // 解析"幻觉部分"
            if (trimmed.contains("幻觉部分") || trimmed.contains("具体")) {
                String parts = trimmed.substring(trimmed.indexOf("：") + 1).trim();
                if (!parts.equals("无")) {
                    details.add(parts);
                }
            }

            // 解析"置信度"
            if (trimmed.contains("置信度")) {
                try {
                    String numStr = trimmed.replaceAll("[^0-9.]", "");
                    confidence = Double.parseDouble(numStr);
                    if (confidence < 0.0) confidence = 0.0;
                    if (confidence > 1.0) confidence = 1.0;
                } catch (NumberFormatException e) {
                    confidence = 0.5;
                }
            }
        }

        return new HallucinationCheckResult(hasHallucination, details, confidence);
    }

    /**
     * 幻觉检查结果
     */
    public static class HallucinationCheckResult {
        private final boolean hasHallucination;
        private final List<String> details;
        private final double confidence;

        public HallucinationCheckResult(boolean hasHallucination, List<String> details, double confidence) {
            this.hasHallucination = hasHallucination;
            this.details = details != null ? List.copyOf(details) : List.of();
            this.confidence = confidence;
        }

        public boolean hasHallucination() { return hasHallucination; }
        public List<String> details() { return details; }
        public double confidence() { return confidence; }

        @Override
        public String toString() {
            return "HallucinationCheckResult{" +
                    "hasHallucination=" + hasHallucination +
                    ", details=" + details +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}