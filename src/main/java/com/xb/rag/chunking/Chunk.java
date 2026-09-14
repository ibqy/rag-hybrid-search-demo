package com.xb.rag.chunking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分块 —— 由若干文档切片（DocSegment）组装而成的语义块
 */
public class Chunk {

    private String id;
    private String docId;
    private String content;
    private List<String> segmentIds;
    private int pageNum;
    private String sectionTitle;
    private int headingLevel;
    private String contentType;
    private Map<String, Object> metadata;
    private int chunkIndex;

    public Chunk() {
        this.segmentIds = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public Chunk(String id, String docId, String content) {
        this.id = id;
        this.docId = docId;
        this.content = content;
        this.segmentIds = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    // ---------- getters ----------

    public String getId() { return id; }

    public String getDocId() { return docId; }

    public String getContent() { return content; }

    public List<String> getSegmentIds() { return segmentIds; }

    public int getPageNum() { return pageNum; }

    public String getSectionTitle() { return sectionTitle; }

    public int getHeadingLevel() { return headingLevel; }

    public String getContentType() { return contentType; }

    public Map<String, Object> getMetadata() { return metadata; }

    public int getChunkIndex() { return chunkIndex; }

    // ---------- fluent setters（链式调用） ----------

    public Chunk setId(String id) { this.id = id; return this; }

    public Chunk setDocId(String docId) { this.docId = docId; return this; }

    public Chunk setContent(String content) { this.content = content; return this; }

    public Chunk setSegmentIds(List<String> segmentIds) { this.segmentIds = segmentIds; return this; }

    public Chunk setPageNum(int pageNum) { this.pageNum = pageNum; return this; }

    public Chunk setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; return this; }

    public Chunk setHeadingLevel(int headingLevel) { this.headingLevel = headingLevel; return this; }

    public Chunk setContentType(String contentType) { this.contentType = contentType; return this; }

    public Chunk setMetadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

    public Chunk setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; return this; }
}