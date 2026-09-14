package com.xb.rag.evaluation;

import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.hallucination.HallucinationDetector.HallucinationCheckResult;
import com.xb.rag.retrieval.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评估执行器 —— 批量运行测试集，计算多个指标
 *
 * 评估维度：
 * - Recall@K: 正确 chunk 是否被召回
 * - Precision@K: 召回结果中正确 chunk 的比例
 * - 幻觉率: LLM 回答中存在幻觉的比例
 */
@Service
public class EvalRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);

    // 模拟检索接口：实际注入 HybridSearchService
    private final java.util.function.Function<String, List<SearchResult>> mockRetriever;

    // 模拟问答接口：实际注入 ChatModel
    private final java.util.function.BiFunction<String, String, String> mockAnswerer;

    private final HallucinationDetector detector;

    public EvalRunner(HallucinationDetector detector) {
        this.detector = detector;
        // 默认模拟实现，仅返回空结果
        this.mockRetriever = q -> List.of();
        this.mockAnswerer = (q, c) -> "";
    }

    /**
     * 注入真正的检索函数（用于测试时替换）
     */
    public EvalRunner withRetriever(java.util.function.Function<String, List<SearchResult>> retriever) {
        return new EvalRunner(this.detector) {
            @Override
            public EvalReport evaluate(EvalDataset dataset, int topK) {
                return EvalRunner.this.evaluateWith(dataset, topK, retriever, mockAnswerer);
            }
        };
    }

    /**
     * 运行全量评估
     */
    public EvalReport evaluate(EvalDataset dataset, int topK) {
        return evaluateWith(dataset, topK, mockRetriever, mockAnswerer);
    }

    private EvalReport evaluateWith(EvalDataset dataset, int topK,
                                     java.util.function.Function<String, List<SearchResult>> retriever,
                                     java.util.function.BiFunction<String, String, String> answerer) {
        if (dataset == null || dataset.size() == 0) {
            return new EvalReport(List.of(), List.of(), 0);
        }

        int totalHits = 0;
        double totalPrecision = 0.0;
        int hallucinationCount = 0;

        for (EvalDataset.EvalSample sample : dataset.getSamples()) {
            // 检索
            List<SearchResult> results = retriever.apply(sample.question());
            Set<String> retrievedIds = results.stream()
                    .map(SearchResult::getChunkId)
                    .collect(Collectors.toSet());

            // 计算 Recall@K
            Set<String> relevant = Set.copyOf(sample.relevantChunkIds());
            long hits = relevant.stream().filter(retrievedIds::contains).count();
            if (!relevant.isEmpty()) {
                totalHits += hits;
                totalPrecision += (double) hits / Math.min(topK, results.size());
            }

            // 检查幻觉
            String context = results.stream()
                    .map(SearchResult::getContent)
                    .collect(Collectors.joining("\n"));
            String answer = answerer.apply(sample.question(), context);
            HallucinationCheckResult check = detector.check(sample.question(), context, answer);
            if (check.hasHallucination()) {
                hallucinationCount++;
            }
        }

        int n = dataset.size();
        double recall = (double) totalHits / dataset.getSamples().stream()
                .mapToLong(s -> s.relevantChunkIds().size()).sum();
        double precisionAvg = totalPrecision / n;
        double hallucinationRate = (double) hallucinationCount / n;

        List<EvalMetrics> metrics = List.of(
                new EvalMetrics("Recall@" + topK, recall, n, totalHits),
                new EvalMetrics("Precision@" + topK, precisionAvg, n, (int) totalPrecision),
                new EvalMetrics("HallucinationRate", hallucinationRate, n, hallucinationCount)
        );

        return new EvalReport(metrics, dataset.getSamples(), hallucinationCount);
    }
}