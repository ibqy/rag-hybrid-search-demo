package com.xb.rag.chunking;

/**
 * 分块配置 —— 控制分块策略的参数
 */
public class ChunkConfig {

    /** 策略名称：hierarchical / semantic / fixed */
    private String strategy;

    /** 目标分块大小（字符数），默认 512 */
    private int chunkSize = 512;

    /** 相邻分块之间的重叠字符数，默认 128 */
    private int chunkOverlap = 128;

    /** 最小分块大小，低于此值的分块被丢弃，默认 50 */
    private int minChunkSize = 50;

    /** 语义分块时使用的嵌入模型名称 */
    private String embedModel;

    public ChunkConfig() {
    }

    public ChunkConfig(String strategy) {
        this.strategy = strategy;
    }

    // ---------- getters / setters ----------

    public String getStrategy() { return strategy; }

    public void setStrategy(String strategy) { this.strategy = strategy; }

    public int getChunkSize() { return chunkSize; }

    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getChunkOverlap() { return chunkOverlap; }

    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }

    public int getMinChunkSize() { return minChunkSize; }

    public void setMinChunkSize(int minChunkSize) { this.minChunkSize = minChunkSize; }

    public String getEmbedModel() { return embedModel; }

    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }
}