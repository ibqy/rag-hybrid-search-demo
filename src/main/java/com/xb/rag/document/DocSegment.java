package com.xb.rag.document;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档切片 —— 从原始文档中提取的一个段落/表格/代码块/图片OCR结果
 *
 * @author ibqy
 */
public class DocSegment {

    private String id;
    private String docId;
    private String content;

    // text | table | code | image_ocr
    private String contentType;

    private Map<String, Object> metadata;
    private int pageNum;
    private String sectionTitle;
    private int headingLevel;

    // 表格类型时使用，markdown 格式的表格文本
    private String tableMarkdown;

    public DocSegment() {
        this.metadata = new HashMap<>();
    }

    public DocSegment(String id, String docId, String content, String contentType) {
        this.id = id;
        this.docId = docId;
        this.content = content;
        this.contentType = contentType;
        this.metadata = new HashMap<>();
    }

    // ---------- getters ----------

    public String getId() { return id; }
    public String getDocId() { return docId; }
    public String getContent() { return content; }
    public String getContentType() { return contentType; }
    public Map<String, Object> getMetadata() { return metadata; }
    public int getPageNum() { return pageNum; }
    public String getSectionTitle() { return sectionTitle; }
    public int getHeadingLevel() { return headingLevel; }
    public String getTableMarkdown() { return tableMarkdown; }

    // ---------- fluent setters (链式调用) ----------

    public DocSegment setId(String id) { this.id = id; return this; }
    public DocSegment setDocId(String docId) { this.docId = docId; return this; }
    public DocSegment setContent(String content) { this.content = content; return this; }
    public DocSegment setContentType(String contentType) { this.contentType = contentType; return this; }
    public DocSegment setMetadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
    public DocSegment setPageNum(int pageNum) { this.pageNum = pageNum; return this; }
    public DocSegment setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; return this; }
    public DocSegment setHeadingLevel(int headingLevel) { this.headingLevel = headingLevel; return this; }
    public DocSegment setTableMarkdown(String tableMarkdown) { this.tableMarkdown = tableMarkdown; return this; }
}