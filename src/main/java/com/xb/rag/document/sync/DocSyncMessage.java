package com.xb.rag.document.sync;

/**
 * 文档同步消息 —— 用于 Kafka 异步文档处理的消息体
 *
 * 上传文档 -> 发送此消息到 Kafka -> 消费者异步解析、切片、写入向量库
 */
public class DocSyncMessage {

    /** 操作类型: ADD / UPDATE / DELETE */
    public enum Action { ADD, UPDATE, DELETE }

    private String docId;
    private String docName;
    private String tenantId;
    private Action action;
    private String storedPath;     // OSS/Minio 存储路径
    private String md5;
    private long timestamp;

    public DocSyncMessage() {}

    public DocSyncMessage(String docId, String docName, String tenantId, Action action, String storedPath) {
        this.docId = docId;
        this.docName = docName;
        this.tenantId = tenantId;
        this.action = action;
        this.storedPath = storedPath;
        this.timestamp = System.currentTimeMillis();
    }

    public String getDocId() { return docId; }
    public String getDocName() { return docName; }
    public String getTenantId() { return tenantId; }
    public Action getAction() { return action; }
    public String getStoredPath() { return storedPath; }
    public String getMd5() { return md5; }
    public long getTimestamp() { return timestamp; }

    public void setDocId(String docId) { this.docId = docId; }
    public void setDocName(String docName) { this.docName = docName; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public void setAction(Action action) { this.action = action; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
    public void setMd5(String md5) { this.md5 = md5; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}