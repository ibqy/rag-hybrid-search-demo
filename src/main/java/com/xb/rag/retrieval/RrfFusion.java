package com.xb.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * RRF（Reciprocal Rank Fusion）融合器
 *
 * 将向量检索和 BM25 检索的结果按倒数排序融合算法合并，
 * 公式：score = 1 / (k + rank)，其中 rank 是各结果在原始列表中的排序位置，
 * k 为平滑常数（默认 60）。
 */
@Component
public class RrfFusion {

    /** 默认的平滑常数 */
    private static final int DEFAULT_K = 60;

    private final int k;

    public RrfFusion() {
        this.k = DEFAULT_K;
    }

    public RrfFusion(int k) {
        this.k = k;
    }

    /**
     * 对向量结果和 BM25 结果执行 RRF 融合
     *
     * @param vectorResults 向量检索结果（需已按相关性排序且 rank 已设置）
     * @param bm25Results   BM25 检索结果（需已按相关性排序且 rank 已设置）
     * @param finalTopK     最终返回的结果数
     * @return 融合后重排序的 SearchResult 列表
     */
    public List<SearchResult> fuse(List<SearchResult> vectorResults,
                                   List<SearchResult> bm25Results,
                                   int finalTopK) {
        // 以 chunkId 为 key 累积 RRF 得分
        Map<String, SearchResult> merged = new HashMap<>();

        // 处理向量检索结果
        for (SearchResult sr : vectorResults) {
            double rrfScore = 1.0 / (k + sr.getRank());
            sr.setScore(rrfScore);
            sr.setSource("fusion");
            merged.put(sr.getChunkId(), sr);
        }

        // 处理 BM25 结果：已有 chunkId 取较高分，没有则新增
        for (SearchResult sr : bm25Results) {
            double rrfScore = 1.0 / (k + sr.getRank());
            String cid = sr.getChunkId();
            SearchResult existing = merged.get(cid);
            if (existing != null) {
                // 保留更高的 RRF 得分
                if (rrfScore > existing.getScore()) {
                    existing.setScore(rrfScore);
                }
            } else {
                sr.setScore(rrfScore);
                sr.setSource("fusion");
                merged.put(cid, sr);
            }
        }

        // 按 RRF 得分降序排列
        List<SearchResult> fused = new ArrayList<>(merged.values());
        fused.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());

        // 重设 rank 并截取 topK
        int rank = 1;
        List<SearchResult> top = new ArrayList<>(Math.min(finalTopK, fused.size()));
        for (SearchResult sr : fused) {
            if (rank > finalTopK) break;
            sr.setRank(rank++);
            top.add(sr);
        }
        return top;
    }
}