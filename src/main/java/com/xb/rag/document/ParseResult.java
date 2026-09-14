package com.xb.rag.document;

import java.util.List;

/**
 * 文档解析结果：元数据 + 切片列表 + 状态
 */
public class ParseResult {

    private DocumentMeta docMeta;
    private List<DocSegment> segments;
    private boolean success;
    private String errorMessage;

    public ParseResult() {}

    public ParseResult(DocumentMeta docMeta, List<DocSegment> segments, boolean success) {
        this.docMeta = docMeta;
        this.segments = segments;
        this.success = success;
    }

    public ParseResult(DocumentMeta docMeta, List<DocSegment> segments, boolean success, String errorMessage) {
        this.docMeta = docMeta;
        this.segments = segments;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    // ---------- getters ----------

    public DocumentMeta getDocMeta() { return docMeta; }
    public List<DocSegment> getSegments() { return segments; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}