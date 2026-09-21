package com.xb.rag.evaluation;

import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.retrieval.HybridSearchService;
import com.xb.rag.retrieval.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * 评估执行器 —— 批量运行测试集，计算检索指标
 *
 * 评估维度：Recall@K, Precision@K, MRR@K, nDCG@K
 */
@Service
public class EvalRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);

    private final Function<String, List<SearchResult>> defaultRetriever;
    private final HallucinationDetector detector;

    @Autowired
    public EvalRunner(HallucinationDetector detector, HybridSearchService hybridSearchService) {
        this.detector = detector;
        this.defaultRetriever = query -> hybridSearchService.search(query);
    }

    private EvalRunner(HallucinationDetector detector, Function<String, List<SearchResult>> retriever) {
        this.detector = detector;
        this.defaultRetriever = retriever;
    }

    public EvalRunner withRetriever(Function<String, List<SearchResult>> retriever) {
        return new EvalRunner(this.detector, retriever);
    }

    public EvalReport evaluate(EvalDataset dataset, int topK) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0, got " + topK);
        }
        if (dataset == null || dataset.size() == 0) {
            throw new IllegalArgumentException("dataset must not be empty");
        }
        for (EvalDataset.EvalSample sample : dataset.getSamples()) {
            if (sample.relevantChunkIds() == null || sample.relevantChunkIds().isEmpty()) {
                throw new IllegalArgumentException("each sample must have at least one relevant chunk id");
            }
            for (String id : sample.relevantChunkIds()) {
                if (id == null || id.isBlank()) {
                    throw new IllegalArgumentException("relevant chunk id must not be blank");
                }
            }
        }

        int n = dataset.size();
        double sumRecall = 0, sumPrecision = 0, sumMRR = 0, sumNDCG = 0;
        List<EvalDataset.EvalSample> samples = dataset.getSamples();
        List<List<String>> retrievedIdsPerSample = new ArrayList<>();

        for (EvalDataset.EvalSample sample : samples) {
            List<SearchResult> results = defaultRetriever.apply(sample.question());
            List<String> truncated = results.stream()
                    .limit(topK)
                    .map(SearchResult::getChunkId)
                    .toList();
            retrievedIdsPerSample.add(truncated);

            Set<String> relevant = new LinkedHashSet<>(sample.relevantChunkIds());
            Set<String> retrieved = new LinkedHashSet<>(truncated);

            long hits = relevant.stream().filter(retrieved::contains).count();
            sumRecall += relevant.isEmpty() ? 0 : (double) hits / relevant.size();
            sumPrecision += (double) hits / topK;

            int firstHit = -1;
            for (int i = 0; i < truncated.size(); i++) {
                if (relevant.contains(truncated.get(i))) {
                    firstHit = i + 1;
                    break;
                }
            }
            sumMRR += firstHit > 0 ? 1.0 / firstHit : 0;
            sumNDCG += computeNDCG(truncated, relevant);
        }

        List<EvalMetrics> metrics = List.of(
                new EvalMetrics("Recall@" + topK, sumRecall / n, n, (int) Math.round(sumRecall * n)),
                new EvalMetrics("Precision@" + topK, sumPrecision / n, n, (int) Math.round(sumPrecision * n)),
                new EvalMetrics("MRR@" + topK, sumMRR / n, n, (int) Math.round(sumMRR * n)),
                new EvalMetrics("nDCG@" + topK, sumNDCG / n, n, (int) Math.round(sumNDCG * n))
        );

        return new EvalReport(metrics, samples, 0, retrievedIdsPerSample);
    }

    private double computeNDCG(List<String> retrieved, Set<String> relevant) {
        double dcg = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < retrieved.size(); i++) {
            String id = retrieved.get(i);
            if (seen.add(id) && relevant.contains(id)) {
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2));
            }
        }
        int idealHits = Math.min(relevant.size(), retrieved.size());
        double idcg = 0;
        for (int i = 0; i < idealHits; i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }
        return idcg > 0 ? dcg / idcg : 0;
    }
}
