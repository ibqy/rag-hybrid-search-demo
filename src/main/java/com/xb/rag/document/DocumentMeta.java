package com.xb.rag.document;

import java.util.List;

/**
 * 文档元信息，记录来源、类型、权限等
 *
 * @author ibqy
 */
public class DocumentMeta {

    private String docId;
    private String docName;
    private String sourcePath;
    private DocType fileType;
    private long fileSize;
    private int pageCount;
    private String author;
    private String createdAt;
    private String updatedAt;
    private String md5;
    private String tenantId;
    private List<String> visibleRoles;

    public DocumentMeta() {}

    public DocumentMeta(String docId, String docName, String sourcePath, DocType fileType) {
        this.docId = docId;
        this.docName = docName;
        this.sourcePath = sourcePath;
        this.fileType = fileType;
    }

    // ---------- getters ----------

    public String getDocId() { return docId; }
    public String getDocName() { return docName; }
    public String getSourcePath() { return sourcePath; }
    public DocType getFileType() { return fileType; }
    public long getFileSize() { return fileSize; }
    public int getPageCount() { return pageCount; }
    public String getAuthor() { return author; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getMd5() { return md5; }
    public String getTenantId() { return tenantId; }
    public List<String> getVisibleRoles() { return visibleRoles; }

    // ---------- setters ----------

    public void setDocId(String docId) { this.docId = docId; }
    public void setDocName(String docName) { this.docName = docName; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
    public void setFileType(DocType fileType) { this.fileType = fileType; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public void setAuthor(String author) { this.author = author; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public void setMd5(String md5) { this.md5 = md5; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setVisibleRoles(List<String> visibleRoles) { this.visibleRoles = visibleRoles; }
}