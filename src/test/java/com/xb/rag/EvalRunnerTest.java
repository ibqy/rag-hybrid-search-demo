package com.xb.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xb.rag.evaluation.EvalDataset;
import com.xb.rag.evaluation.EvalReport;
import com.xb.rag.evaluation.EvalRunner;
import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.retrieval.HybridSearchService;
import com.xb.rag.retrieval.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvalRunnerTest {

    @Test
    void excludesHitsBeyondTopK() {
        var report = runner(List.of(hit("wrong"), hit("right"))).evaluate(dataset("right"), 1);
        assertThat(metric(report, "Recall@1")).isZero();
        assertThat(metric(report, "Precision@1")).isZero();
    }

    @Test
    void emptyRetrievalProducesFiniteZeroMetrics() {
        var report = runner(List.of()).evaluate(dataset("right"), 3);
        assertThat(metric(report, "Recall@3")).isZero();
        assertThat(metric(report, "Precision@3")).isZero();
    }

    @Test
    void precisionUsesRequestedKWhenFewerResultsAreReturned() {
        var report = runner(List.of(hit("right"))).evaluate(dataset("right"), 4);
        assertThat(metric(report, "Precision@4")).isEqualTo(0.25);
    }

    @Test
    void repeatedJudgmentsDoNotChangeRecall() {
        var report = runner(List.of(hit("right"))).evaluate(dataset("right", "right"), 1);
        assertThat(metric(report, "Recall@1")).isEqualTo(1.0);
    }

    @Test
    void reportsRankSensitiveMetricsWithoutInventingAnswerQuality() {
        var report = runner(List.of(hit("wrong"), hit("right"))).evaluate(dataset("right"), 3);
        assertThat(metric(report, "MRR@3")).isEqualTo(0.5);
        assertThat(metric(report, "nDCG@3")).isCloseTo(0.6309297535714574, within(1e-12));
        assertThat(report.metrics()).noneMatch(m -> m.metricName().equals("HallucinationRate"));
    }

    @Test
    void rejectsUndefinedEvaluationInputs() {
        assertThatIllegalArgumentException().isThrownBy(() -> runner(List.of()).evaluate(dataset("right"), 0));
        assertThatIllegalArgumentException().isThrownBy(() -> runner(List.of()).evaluate(new EvalDataset(), 3));
        assertThatIllegalArgumentException().isThrownBy(() -> runner(List.of()).evaluate(dataset(), 3));
    }

    @Test
    void reportIsSerializableAsAnApiResponse() throws Exception {
        var report = runner(List.of(hit("right"))).evaluate(dataset("right"), 1);
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(report));
        assertThat(json.path("metrics").size()).isEqualTo(4);
        assertThat(json.path("metrics").get(0).path("value").asDouble()).isEqualTo(1.0);
    }

    private EvalRunner runner(List<SearchResult> hits) {
        return new EvalRunner(mock(HallucinationDetector.class, RETURNS_DEEP_STUBS), mock(HybridSearchService.class)).withRetriever(q -> hits);
    }

    private EvalDataset dataset(String... relevant) {
        return new EvalDataset(List.of(new EvalDataset.EvalSample("question", List.of(relevant), "reference")));
    }

    private SearchResult hit(String id) {
        return new SearchResult(id, "doc", "content");
    }

    private double metric(EvalReport report, String name) {
        return report.metrics().stream().filter(m -> m.metricName().equals(name)).findFirst().orElseThrow().value();
    }
}
