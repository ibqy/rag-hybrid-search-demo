package com.xb.rag;

import com.xb.rag.retrieval.RrfFusion;
import com.xb.rag.retrieval.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * RRF 融合算法单元测试 —— 验证多路排名融合的打分、去重、截断逻辑
 */
class RrfFusionTest {

    @Test
    void sumsEvidenceFromBothRankings() {
        var results = new RrfFusion().fuse(
                List.of(hit("a", 1), hit("b", 2)),
                List.of(hit("b", 1), hit("c", 2)), 3);

        assertThat(results).extracting(SearchResult::getChunkId).containsExactly("b", "a", "c");
        assertThat(results.getFirst().getScore()).isCloseTo(0.03252247488101534, within(1e-12));
        assertThat(results).extracting(SearchResult::getRank).containsExactly(1, 2, 3);
    }

    @Test
    void preservesInputsAndCitationMetadataAcrossRepeatedExperiments() {
        var original = hit("a", 1).setScore(0.9).setSource("vector")
                .setDocName("manual").setSectionTitle("intro").setPageNum(7)
                .setMetadata(Map.of("tenantId", "demo"));
        var fusion = new RrfFusion();
        var first = fusion.fuse(List.of(original), List.of(), 1).getFirst();
        var second = fusion.fuse(List.of(original), List.of(), 1).getFirst();

        assertThat(original.getScore()).isEqualTo(0.9);
        assertThat(original.getSource()).isEqualTo("vector");
        assertThat(first).isNotSameAs(original);
        assertThat(second.getScore()).isEqualTo(first.getScore());
        assertThat(first.getDocName()).isEqualTo("manual");
        assertThat(first.getSectionTitle()).isEqualTo("intro");
        assertThat(first.getPageNum()).isEqualTo(7);
        assertThat(first.getDocId()).isEqualTo("doc");
        assertThat(first.getContent()).isEqualTo("content a");
        first.getMetadata().put("tenantId", "changed");
        assertThat(original.getMetadata()).containsEntry("tenantId", "demo");
    }

    @Test
    void usesListPositionsRatherThanStaleRankFields() {
        var results = new RrfFusion().fuse(List.of(hit("z", 99), hit("a", 0)), List.of(), 2);
        assertThat(results).extracting(SearchResult::getChunkId).containsExactly("z", "a");
        assertThat(results.getFirst().getScore()).isCloseTo(0.01639344262295082, within(1e-12));
    }

    @Test
    void countsOnlyFirstOccurrencePerRouteWithoutPromotingLaterPositions() {
        var results = new RrfFusion().fuse(
                List.of(hit("a", 1), hit("a", 2), hit("b", 3)), List.of(), 2);
        assertThat(results).extracting(SearchResult::getChunkId).containsExactly("a", "b");
        assertThat(results.getFirst().getScore()).isCloseTo(0.01639344262295082, within(1e-12));
        assertThat(results.get(1).getScore()).isCloseTo(0.01587301587301587, within(1e-12));
    }

    @Test
    void breaksTiesByChunkIdAndLimitsResults() {
        var fusion = new RrfFusion();
        assertThat(fusion.fuse(List.of(hit("z", 1)), List.of(hit("a", 1)), 1))
                .extracting(SearchResult::getChunkId).containsExactly("a");
        assertThat(fusion.fuse(List.of(), List.of(), 2)).isEmpty();
    }

    @Test
    void rejectsInvalidFusionParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RrfFusion(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new RrfFusion(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RrfFusion().fuse(List.of(), List.of(), 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new RrfFusion().fuse(List.of(), List.of(), -1));
    }

    private SearchResult hit(String id, int rank) {
        return new SearchResult(id, "doc", "content " + id).setRank(rank);
    }
}
