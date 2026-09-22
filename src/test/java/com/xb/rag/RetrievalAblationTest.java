package com.xb.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xb.rag.evaluation.EvalDataset;
import com.xb.rag.evaluation.EvalReport;
import com.xb.rag.evaluation.EvalRunner;
import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.retrieval.HybridSearchService;
import com.xb.rag.retrieval.RrfFusion;
import com.xb.rag.retrieval.SearchResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 检索消融实验测试 —— 对比向量/BM25/RRF 三路检索效果
 */
class RetrievalAblationTest {

    @Test
    void comparesFrozenRankingsAndExportsAuditableResults() throws Exception {
        byte[] bytes;
        try (var input = getClass().getResourceAsStream("/retrieval-snapshot.json")) {
            bytes = input.readAllBytes();
        }
        var mapper = new ObjectMapper();
        var snapshot = mapper.readValue(bytes, Snapshot.class);
        var dataset = new EvalDataset(snapshot.queries().stream()
                .map(q -> new EvalDataset.EvalSample(q.question(), q.relevantChunkIds(), null)).toList());
        Map<String, List<SearchResult>> vector = new LinkedHashMap<>();
        Map<String, List<SearchResult>> bm25 = new LinkedHashMap<>();
        Map<String, List<SearchResult>> hybrid = new LinkedHashMap<>();
        var fusion = new RrfFusion(snapshot.rankConstant());
        for (var query : snapshot.queries()) {
            var vectorHits = hits(query.vector());
            var bm25Hits = hits(query.bm25());
            vector.put(query.question(), vectorHits);
            bm25.put(query.question(), bm25Hits);
            hybrid.put(query.question(), fusion.fuse(vectorHits, bm25Hits, snapshot.topK()));
        }
        Map<String, EvalReport> reports = new LinkedHashMap<>();
        reports.put("vector", evaluate(dataset, snapshot.topK(), vector));
        reports.put("bm25", evaluate(dataset, snapshot.topK(), bm25));
        reports.put("rrf", evaluate(dataset, snapshot.topK(), hybrid));

        assertThat(metric(reports.get("vector"), "Recall@2")).isEqualTo(0.625);
        assertThat(metric(reports.get("bm25"), "Recall@2")).isEqualTo(0.375);
        assertThat(metric(reports.get("rrf"), "Recall@2")).isEqualTo(0.625);
        assertThat(metric(reports.get("vector"), "MRR@2")).isEqualTo(0.625);
        assertThat(metric(reports.get("rrf"), "MRR@2")).isEqualTo(0.5);
        assertThat(hybrid.get(snapshot.queries().get(1).question()))
                .extracting(SearchResult::getChunkId).containsExactly("common", "recovery");
        assertThat(vector.get(snapshot.queries().getFirst().question()).getFirst().getScore()).isEqualTo(1.0);

        var output = Path.of("target", "evaluation", "ablation.json");
        Files.createDirectories(output.getParent());
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("snapshot", snapshot);
        artifact.put("snapshotSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        artifact.put("evaluationScope", "binary relevance; macro averages; fixed K; no LLM answer evaluation");
        artifact.put("reports", reports);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), artifact);
        System.out.println("Offline retrieval ablation: " + output.toAbsolutePath());
        reports.forEach((name, report) -> System.out.println(name + "\n" + report.toReportString()));
    }

    private EvalReport evaluate(EvalDataset dataset, int topK, Map<String, List<SearchResult>> rankings) {
        return new EvalRunner(mock(HallucinationDetector.class, RETURNS_DEEP_STUBS), mock(HybridSearchService.class))
                .withRetriever(rankings::get).evaluate(dataset, topK);
    }

    private List<SearchResult> hits(List<String> ids) {
        return java.util.stream.IntStream.range(0, ids.size())
                .mapToObj(i -> new SearchResult(ids.get(i), "fixture", "synthetic evidence")
                        .setRank(i + 1).setScore(1.0 / (i + 1))).toList();
    }

    private double metric(EvalReport report, String name) {
        return report.metrics().stream().filter(m -> m.metricName().equals(name)).findFirst().orElseThrow().value();
    }

    record Snapshot(String snapshotId, String provenance, int topK, int rankConstant, List<Query> queries) {}
    record Query(String question, String category, List<String> relevantChunkIds, List<String> vector, List<String> bm25) {}
}
