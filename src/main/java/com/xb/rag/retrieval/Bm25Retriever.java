package com.xb.rag.retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

/**
 * BM25 关键词检索服务 —— 基于 Elasticsearch 的 multi-match 查询
 *
 * 同时对 content（文档正文）和 sectionTitle（章节标题）字段执行
 * 全文检索，适合处理精确关键词匹配场景。
 *
 * @author ibqy
 */
@Service
public class Bm25Retriever {

    /** 默认索引名 */
    private static final String DEFAULT_INDEX = "rag_chunks";

    private final ElasticsearchClient esClient;

    public Bm25Retriever(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 使用默认索引名执行 BM25 检索
     */
    public List<SearchResult> search(String query, int topK) {
        return search(query, topK, DEFAULT_INDEX);
    }

    /**
     * 在指定索引上执行 BM25 检索
     *
     * @param query     查询文本
     * @param topK      返回的最大结果数
     * @param indexName Elasticsearch 索引名
     * @return 转换后的 SearchResult 列表
     */
    public List<SearchResult> search(String query, int topK, String indexName) {
        // 构建 multi-match 查询：同时对 content 和 sectionTitle 字段评分
        Query esQuery = Query.of(q -> q
                .multiMatch(mm -> mm
                        .query(query)
                        .fields("content", "sectionTitle")
                )
        );

        try {
            SearchResponse<Map<String, Object>> response = esClient.search(s -> s
                            .index(indexName)
                            .query(esQuery)
                            .size(topK),
                    // 将 ES 文档解析为 Map，便于读取字段
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return convertHits(response.hits().hits());
        } catch (IOException e) {
            throw new RuntimeException("ES 检索失败: " + e.getMessage(), e);
        }
    }

    // ---------- 内部辅助 ----------

    /**
     * 将 ES 的 Hit 列表转为 SearchResult
     */
    private List<SearchResult> convertHits(List<Hit<Map<String, Object>>> hits) {
        List<SearchResult> results = new ArrayList<>(hits.size());
        int rank = 1;
        for (Hit<Map<String, Object>> hit : hits) {
            String chunkId = hit.id();
            Map<String, Object> source = hit.source();
            double score = hit.score() != null ? hit.score() : 0.0;

            SearchResult sr = new SearchResult(
                    chunkId,
                    strField(source, "docId"),
                    strField(source, "content")
            )
                    .setSectionTitle(strField(source, "sectionTitle"))
                    .setPageNum(intField(source, "pageNum"))
                    .setScore(score)
                    .setSource("bm25")
                    .setRank(rank++)
                    .setMetadata(source);
            results.add(sr);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sourceAsMap(Hit<Map<String, Object>> hit) {
        return hit.source();
    }

    private static String strField(Map<String, Object> map, String key) {
        Object v = map != null ? map.get(key) : null;
        return v != null ? v.toString() : "";
    }

    private static int intField(Map<String, Object> map, String key) {
        Object v = map != null ? map.get(key) : null;
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}