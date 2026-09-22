package com.xb.rag.retrieval;

import java.util.HashMap;
import java.util.Map;

/**
 * 检索结果模型 —— 统一表示向量检索 / BM25 / Rerank 等各阶段的匹配结果
 *
 * @author ibqy
 */
public class SearchResult {

    private String chunkId;
    private String docId;
    private String docName;
    private String content;
    private String sectionTitle;
    private int pageNum;
    private double score;
    private String source;          // "vector" / "bm25" / "rerank" / "fusion"
    private int rank;
    private Map<String, Object> metadata;

    public SearchResult() {
        this.metadata = new HashMap<>();
    }

    public SearchResult(String chunkId, String docId, String content) {
        this.chunkId = chunkId;
        this.docId = docId;
        this.content = content;
        this.metadata = new HashMap<>();
    }

    public String getChunkId() { return chunkId; }
    public String getDocId() { return docId; }
    public String getDocName() { return docName; }
    public String getContent() { return content; }
    public String getSectionTitle() { return sectionTitle; }
    public int getPageNum() { return pageNum; }
    public double getScore() { return score; }
    public String getSource() { return source; }
    public int getRank() { return rank; }
    public Map<String, Object> getMetadata() { return metadata; }

    public SearchResult setChunkId(String chunkId) { this.chunkId = chunkId; return this; }
    public SearchResult setDocId(String docId) { this.docId = docId; return this; }
    public SearchResult setDocName(String docName) { this.docName = docName; return this; }
    public SearchResult setContent(String content) { this.content = content; return this; }
    public SearchResult setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; return this; }
    public SearchResult setPageNum(int pageNum) { this.pageNum = pageNum; return this; }
    public SearchResult setScore(double score) { this.score = score; return this; }
    public SearchResult setSource(String source) { this.source = source; return this; }
    public SearchResult setRank(int rank) { this.rank = rank; return this; }
    public SearchResult setMetadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
}