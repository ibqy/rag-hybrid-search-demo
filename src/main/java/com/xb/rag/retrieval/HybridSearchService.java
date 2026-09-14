package com.xb.rag.retrieval;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 混合检索编排服务
 *
 * 将向量检索与 BM25 关键词检索的结果通过 RRF 融合，
 * 提供统一的高层检索入口。
 */
@Service
public class HybridSearchService {

    private final VectorRetriever vectorRetriever;
    private final Bm25Retriever bm25Retriever;
    private final RrfFusion rrfFusion;

    /** 默认向量检索 topK */
    @Value("${rag.hybrid.vector-top-k:10}")
    private int defaultVectorTopK;

    /** 默认 BM25 检索 topK */
    @Value("${rag.hybrid.bm25-top-k:10}")
    private int defaultBm25TopK;

    /** 默认融合后返回 topK */
    @Value("${rag.hybrid.final-top-k:5}")
    private int defaultFinalTopK;

    public HybridSearchService(VectorRetriever vectorRetriever,
                               Bm25Retriever bm25Retriever,
                               RrfFusion rrfFusion) {
        this.vectorRetriever = vectorRetriever;
        this.bm25Retriever = bm25Retriever;
        this.rrfFusion = rrfFusion;
    }

    /**
     * 混合检索 —— 依次执行向量检索、BM25 检索、RRF 融合
     *
     * @param query      查询文本
     * @param vectorTopK 向量检索返回数
     * @param bm25TopK   BM25 检索返回数
     * @param finalTopK  融合后最终返回数
     * @return 融合后的 SearchResult 列表
     */
    public List<SearchResult> search(String query, int vectorTopK,
                                     int bm25TopK, int finalTopK) {
        // 1. 向量语义检索
        List<SearchResult> vectorResults = vectorRetriever.search(query, vectorTopK);

        // 2. BM25 关键词检索
        List<SearchResult> bm25Results = bm25Retriever.search(query, bm25TopK);

        // 3. RRF 融合重排序
        return rrfFusion.fuse(vectorResults, bm25Results, finalTopK);
    }

    /**
     * 使用配置文件中的默认参数执行混合检索
     *
     * @param query 查询文本
     * @return 融合后的 SearchResult 列表
     */
    public List<SearchResult> search(String query) {
        return search(query, defaultVectorTopK, defaultBm25TopK, defaultFinalTopK);
    }
}