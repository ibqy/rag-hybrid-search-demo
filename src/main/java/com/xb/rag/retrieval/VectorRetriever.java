package com.xb.rag.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * 向量检索服务 —— 基于 Spring AI VectorStore 实现语义相似度搜索
 *
 * 注入 PgVectorStore（或任何 VectorStore 实现），执行向量检索后
 * 将 Spring AI 的 Document 转为本模块的 SearchResult 模型。
 *
 * @author ibqy
 */
@Service
public class VectorRetriever {

    /** 默认相似度阈值：低于此值的结果将被过滤 */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.0;

    private final VectorStore vectorStore;

    public VectorRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 执行向量检索
     *
     * @param query  查询文本
     * @param topK   返回的最相似结果数
     * @param filter 元数据过滤条件（key=value，例如 tenantId=abc）；传 null 或空时不加过滤
     * @return 转换后的 SearchResult 列表
     */
    public List<SearchResult> search(String query, int topK, Map<String, String> filter) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD);

        // 将 Map<String, String> 转为 Filter Expression DSL 字符串
        if (filter != null && !filter.isEmpty()) {
            String filterExpr = buildFilterExpression(filter);
            builder.filterExpression(filterExpr);
        }

        List<Document> docs = vectorStore.similaritySearch(builder.build());
        return convertResults(docs);
    }

    /**
     * 简化重载 —— 不传 filter 时不做元数据过滤
     */
    public List<SearchResult> search(String query, int topK) {
        return search(query, topK, null);
    }

    // ---------- 内部辅助 ----------

    /**
     * 将 Map 拼装成 Filter Expression 字符串
     *
     * 格式：key1 == 'val1' && key2 == 'val2'
     * Spring AI Filter 语法要求字符串值用单引号包裹。
     */
    private String buildFilterExpression(Map<String, String> filter) {
        return filter.entrySet().stream()
                .map(e -> e.getKey() + " == '" + e.getValue() + "'")
                .collect(Collectors.joining(" && "));
    }

    /**
     * 将 Spring AI Document 转为本地 SearchResult
     */
    private List<SearchResult> convertResults(List<Document> docs) {
        List<SearchResult> results = new ArrayList<>(docs.size());
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            Map<String, Object> meta = doc.getMetadata();

            SearchResult sr = new SearchResult(doc.getId(), strMeta(meta, "docId"), doc.getText())
                    .setSectionTitle(strMeta(meta, "sectionTitle"))
                    .setPageNum(intMeta(meta, "pageNum"))
                    .setScore(extractScore(doc))
                    .setSource("vector")
                    .setRank(i + 1)
                    .setMetadata(meta);
            results.add(sr);
        }
        return results;
    }

    /**
     * 从 Document 的 score 属性取值（Spring AI 在搜索结果中会填充 score）
     */
    private double extractScore(Document doc) {
        Double score = doc.getScore();
        return score != null ? score : 0.0;
    }

    private static String strMeta(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        return v != null ? v.toString() : "";
    }

    private static int intMeta(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}