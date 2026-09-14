package com.xb.rag.controller;

import com.xb.rag.context.ContextBuilder;
import com.xb.rag.hallucination.CitationValidator;
import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.hallucination.PromptConstants;
import com.xb.rag.retrieval.HybridSearchService;
import com.xb.rag.retrieval.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * RAG 问答接口 —— 混合检索 + Rerank + 上下文组装 + LLM 回答
 *
 * POST /api/rag/ask
 * {
 *   "question": "用户问题",
 *   "topK": 8,
 *   "enableHallucinationCheck": true,
 *   "tenantId": "tenant_001"
 * }
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final HybridSearchService hybridSearch;
    private final ChatModel chatModel;
    private final HallucinationDetector detector;
    private final CitationValidator citationValidator;

    @Value("${retrieval.final-top-k:8}")
    private int defaultTopK;

    @Value("${retrieval.score-threshold:0.45}")
    private double scoreThreshold;

    public RagController(HybridSearchService hybridSearch,
                         ChatModel chatModel,
                         HallucinationDetector detector,
                         CitationValidator citationValidator) {
        this.hybridSearch = hybridSearch;
        this.chatModel = chatModel;
        this.detector = detector;
        this.citationValidator = citationValidator;
    }

    /**
     * RAG 问答
     */
    @PostMapping("/ask")
    public Mono<RagResponse> ask(@RequestBody RagRequest request) {
        return Mono.fromCallable(() -> {
            long start = System.currentTimeMillis();

            // 1. 混合检索
            List<SearchResult> results = hybridSearch.search(
                    request.question(),
                    request.vectorTopK() > 0 ? request.vectorTopK() : 20,
                    request.bm25TopK() > 0 ? request.bm25TopK() : 20,
                    request.topK() > 0 ? request.topK() : defaultTopK);

            // 2. 检查是否有有效结果
            if (results == null || results.isEmpty()) {
                return new RagResponse(request.question(), PromptConstants.REFUSAL_MESSAGE,
                        List.of(), 0, System.currentTimeMillis() - start);
            }

            // 3. 构建带引用的上下文
            String context = ContextBuilder.buildContextWithMeta(results);
            List<ContextBuilder.SearchResultCitation> citations =
                    ContextBuilder.buildCitations(results);

            // 4. 组装 prompt + 调用 LLM
            PromptTemplate template = new PromptTemplate(PromptConstants.SYSTEM_PROMPT);
            Prompt prompt = template.create(Map.of("context", context, "question", request.question()));
            String answer = chatModel.call(prompt).getResult().getOutput().getContent();

            // 5. 可选：幻觉检测
            boolean hasHallucination = false;
            String hallucinationDetail = "";
            if (request.enableHallucinationCheck()) {
                var check = detector.check(request.question(), context, answer);
                hasHallucination = check.hasHallucination();
                hallucinationDetail = String.join("; ", check.details());
            }

            // 6. 验证引用有效性
            String validatedAnswer = citationValidator.validateCitations(answer, citations);

            log.info("RAG 问答完成: question={},耗时={}ms, 幻觉={}",
                    request.question(), System.currentTimeMillis() - start, hasHallucination);

            return new RagResponse(request.question(), validatedAnswer, citations,
                    hasHallucination ? 1 : 0, System.currentTimeMillis() - start);
        });
    }

    // ---- 请求/响应模型 ----

    public record RagRequest(
            String question,
            int topK,
            int vectorTopK,
            int bm25TopK,
            boolean enableHallucinationCheck,
            String tenantId) {
        public RagRequest {
            if (question == null || question.isBlank()) {
                throw new IllegalArgumentException("question 不能为空");
            }
        }
    }

    public record RagResponse(
            String question,
            String answer,
            List<ContextBuilder.SearchResultCitation> citations,
            int hasHallucination,
            long costMs) {}
}