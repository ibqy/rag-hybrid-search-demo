package com.xb.rag.retrieval;

import java.util.*;

import org.springframework.stereotype.Component;

/**
 * RRF（Reciprocal Rank Fusion）融合器
 *
 * 公式：score = Σ 1 / (k + position)，position 为各路线列表中的 1-based 下标，
 * k 为平滑常数（默认 60）。同一路线中同一 chunkId 仅首次出现计分。
 *
 * @author ibqy
 */
@Component
public class RrfFusion {

    private static final int DEFAULT_K = 60;

    private final int k;

    public RrfFusion() {
        this.k = DEFAULT_K;
    }

    public RrfFusion(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("rank constant k must be > 0, got " + k);
        }
        this.k = k;
    }

    /**
     * 执行 RRF 融合：将向量检索和 BM25 两路结果按排名倒数加权求和，
     * 返回按融合分数降序排列的 topK 结果。
     *
     * @param vectorResults 向量检索返回的结果列表
     * @param bm25Results   BM25 检索返回的结果列表
     * @param finalTopK     融合后最终返回的结果数
     * @return 融合排序后的 SearchResult 列表
     */
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> bm25Results,
                                   int finalTopK) {
        if (finalTopK <= 0) {
            throw new IllegalArgumentException("finalTopK must be > 0, got " + finalTopK);
        }

        Map<String, Double> scores = new HashMap<>();
        Map<String, SearchResult> prototypes = new HashMap<>();

        accumulate(vectorResults, scores, prototypes);
        accumulate(bm25Results, scores, prototypes);

        List<Map.Entry<String, Double>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort(Comparator.<Map.Entry<String, Double>, Double>comparing(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));

        List<SearchResult> top = new ArrayList<>(Math.min(finalTopK, ranked.size()));
        int rank = 1;
        for (var entry : ranked) {
            if (rank > finalTopK) break;
            SearchResult copy = copyOf(entry.getKey(), prototypes.get(entry.getKey()));
            copy.setScore(entry.getValue());
            copy.setSource("fusion");
            copy.setRank(rank++);
            top.add(copy);
        }
        return top;
    }

    private void accumulate(List<SearchResult> route,
                            Map<String, Double> scores,
                            Map<String, SearchResult> prototypes) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < route.size(); i++) {
            SearchResult sr = route.get(i);
            String cid = sr.getChunkId();
            if (!seen.add(cid)) continue;
            double rrf = 1.0 / (k + (i + 1));
            scores.merge(cid, rrf, Double::sum);
            prototypes.putIfAbsent(cid, sr);
        }
    }

    private SearchResult copyOf(String chunkId, SearchResult src) {
        SearchResult copy = new SearchResult(chunkId, src.getDocId(), src.getContent());
        copy.setDocName(src.getDocName());
        copy.setSectionTitle(src.getSectionTitle());
        copy.setPageNum(src.getPageNum());
        copy.setRank(src.getRank());
        copy.setScore(src.getScore());
        copy.setSource(src.getSource());
        if (src.getMetadata() != null) {
            copy.setMetadata(new HashMap<>(src.getMetadata()));
        }
        return copy;
    }
}
