package com.xb.rag.vectorstore;

/**
 * 向量库索引配置 —— 封装 HNSW / IVF 索引参数
 *
 * @author ibqy
 */
public class VectorStoreConfig {

    public enum StoreType { PGVECTOR, MILVUS }
    public enum IndexType { HNSW, IVFFLAT, IVF_SQ8 }

    private StoreType storeType;
    private IndexType indexType;
    private int dimension;
    private String collectionName;
    private String partitionKey;

    // HNSW 参数
    private int hnswM;
    private int hnswEfConstruction;
    private int hnswEfSearch;

    // IVF 参数
    private int ivfNlist;
    private int ivfNprobe;

    private int ttlDays;          // 自动过期天数
    private boolean enableColdStorage;

    public VectorStoreConfig() {}

    // ---- constructor with common fields ----
    public VectorStoreConfig(StoreType storeType, IndexType indexType, int dimension) {
        this.storeType = storeType;
        this.indexType = indexType;
        this.dimension = dimension;
        // 默认值
        this.hnswM = 16;
        this.hnswEfConstruction = 200;
        this.hnswEfSearch = 50;
        this.ivfNlist = 100;
        this.ivfNprobe = 10;
        this.ttlDays = 0;
        this.enableColdStorage = false;
    }

    // ---- getters ----
    public StoreType getStoreType() { return storeType; }
    public IndexType getIndexType() { return indexType; }
    public int getDimension() { return dimension; }
    public String getCollectionName() { return collectionName; }
    public String getPartitionKey() { return partitionKey; }
    public int getHnswM() { return hnswM; }
    public int getHnswEfConstruction() { return hnswEfConstruction; }
    public int getHnswEfSearch() { return hnswEfSearch; }
    public int getIvfNlist() { return ivfNlist; }
    public int getIvfNprobe() { return ivfNprobe; }
    public int getTtlDays() { return ttlDays; }
    public boolean isEnableColdStorage() { return enableColdStorage; }

    // ---- fluent setters ----
    public VectorStoreConfig setStoreType(StoreType storeType) { this.storeType = storeType; return this; }
    public VectorStoreConfig setIndexType(IndexType indexType) { this.indexType = indexType; return this; }
    public VectorStoreConfig setDimension(int dimension) { this.dimension = dimension; return this; }
    public VectorStoreConfig setCollectionName(String collectionName) { this.collectionName = collectionName; return this; }
    public VectorStoreConfig setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; return this; }
    public VectorStoreConfig setHnswM(int hnswM) { this.hnswM = hnswM; return this; }
    public VectorStoreConfig setHnswEfConstruction(int hnswEfConstruction) { this.hnswEfConstruction = hnswEfConstruction; return this; }
    public VectorStoreConfig setHnswEfSearch(int hnswEfSearch) { this.hnswEfSearch = hnswEfSearch; return this; }
    public VectorStoreConfig setIvfNlist(int ivfNlist) { this.ivfNlist = ivfNlist; return this; }
    public VectorStoreConfig setIvfNprobe(int ivfNprobe) { this.ivfNprobe = ivfNprobe; return this; }
    public VectorStoreConfig setTtlDays(int ttlDays) { this.ttlDays = ttlDays; return this; }
    public VectorStoreConfig setEnableColdStorage(boolean enableColdStorage) { this.enableColdStorage = enableColdStorage; return this; }
}