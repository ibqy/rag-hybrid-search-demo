package com.xb.rag;

import com.xb.rag.controller.EvalController;
import com.xb.rag.evaluation.EvalRunner;
import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.retrieval.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.mockito.Mockito.*;

class EvalControllerTest {

    private final EvalRunner runner = new EvalRunner(mock(HallucinationDetector.class, RETURNS_DEEP_STUBS))
            .withRetriever(q -> List.of(new SearchResult("right", "doc", "content")));
    private final WebTestClient client = WebTestClient.bindToController(new EvalController(runner)).build();

    @Test
    void returnsActualRetrievalMetricsAsJson() {
        client.post().uri("/api/eval/run").header("Content-Type", "application/json")
                .bodyValue("""
                        {"samples":[{"question":"question","relevantChunkIds":["right"]}],"topK":2}
                        """)
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.metrics[0].value").isEqualTo(1.0)
                .jsonPath("$.metrics[1].value").isEqualTo(0.5)
                .jsonPath("$.samples[0].retrievedChunkIds[0]").isEqualTo("right")
                .jsonPath("$.hallucinationCount").doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"samples\":[],\"topK\":2}",
            "{\"topK\":2}",
            "{\"samples\":[null],\"topK\":2}",
            "{\"samples\":[{\"question\":\"q\",\"relevantChunkIds\":[\"a\"]}],\"topK\":0}",
            "{\"samples\":[{\"question\":\"q\",\"relevantChunkIds\":[\"a\"]}],\"topK\":101}",
            "{\"samples\":[{\"question\":\" \",\"relevantChunkIds\":[\"a\"]}],\"topK\":2}",
            "{\"samples\":[{\"question\":\"q\",\"relevantChunkIds\":[]}],\"topK\":2}",
            "{\"samples\":[{\"question\":\"q\",\"relevantChunkIds\":[null]}],\"topK\":2}"
    })
    void invalidEvaluationRequestIsRejectedBeforeRetrieval(String body) {
        client.post().uri("/api/eval/run").header("Content-Type", "application/json")
                .bodyValue(body).exchange().expectStatus().isBadRequest();
    }
}
