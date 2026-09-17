package com.xb.rag.controller;

import com.xb.rag.context.ContextBuilder;
import com.xb.rag.hallucination.CitationValidator;
import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.hallucination.PromptConstants;
import com.xb.rag.retrieval.HybridSearchService;
import com.xb.rag.retrieval.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    private final HybridSearchService hybridSearch;
    private final ChatModel chatModel;
    private final HallucinationDetector detector;

    @Value("${retrieval.final-top-k:8}")
    private int defaultTopK;

    public RagController(HybridSearchService hybridSearch, ChatModel chatModel,
                         HallucinationDetector detector) {
        this.hybridSearch = hybridSearch;
        this.chatModel = chatModel;
        this.detector = detector;
    }

    @PostMapping("/ask")
    public Mono<RagResponse> ask(@RequestBody RagRequest request) {
        return Mono.fromCallable(() -> {
            long start = System.currentTimeMillis();
            List<SearchResult> results = hybridSearch.search(
                    request.question(),
                    request.vectorTopK() > 0 ? request.vectorTopK() : 20,
                    request.bm25TopK() > 0 ? request.bm25TopK() : 20,
                    request.topK() > 0 ? request.topK() : defaultTopK);

            if (results == null || results.isEmpty()) {
                return new RagResponse(request.question(), PromptConstants.REJECTION_MESSAGE,
                        List.of(), 0, System.currentTimeMillis() - start);
            }

            String context = ContextBuilder.buildContextWithMeta(results);
            List<ContextBuilder.SearchResultCitation> citations = ContextBuilder.buildCitations(results);
            PromptTemplate template = new PromptTemplate(PromptConstants.SYSTEM_PROMPT);
            Prompt prompt = template.create(Map.of("context", context, "question", request.question()));
            String answer = chatModel.call(prompt).getResult().getOutput().getText();

            boolean hasHallucination = false;
            if (request.enableHallucinationCheck()) {
                hasHallucination = detector.check(request.question(), context, answer).hasHallucination();
            }
            boolean invalidCitations = !CitationValidator.validateCitations(answer, results).isEmpty();
            String validatedAnswer = invalidCitations ? PromptConstants.REJECTION_MESSAGE : answer;

            log.info("RAG 问答完成: 耗时={}ms, 幻觉={}, 无效引用={}",
                    System.currentTimeMillis() - start, hasHallucination, invalidCitations);
            return new RagResponse(request.question(), validatedAnswer, citations,
                    hasHallucination || invalidCitations ? 1 : 0, System.currentTimeMillis() - start);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public record RagRequest(String question, int topK, int vectorTopK, int bm25TopK,
                             boolean enableHallucinationCheck, String tenantId) {
        public RagRequest {
            if (question == null || question.isBlank()) {
                throw new IllegalArgumentException("question 不能为空");
            }
        }
    }

    public record RagResponse(String question, String answer,
                              List<ContextBuilder.SearchResultCitation> citations,
                              int hasHallucination, long costMs) {}
}
