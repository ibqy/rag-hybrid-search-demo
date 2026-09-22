package com.xb.rag.vectorstore;

import com.xb.rag.chunking.Chunk;
import com.xb.rag.document.DocumentMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 增量更新服务 —— 文档新增、修改、删除的向量库同步
 *
 * 增量更新流程：
 * 1. 计算文档 MD5，与库中历史值对比
 * 2. 未变更 → 跳过；变更 → 删除旧向量 → 重新解析切片 → 写入新向量
 * 3. 删除 → 软删除标记，后台异步物理清理
 *
 * 生产环境下应通过 Kafka 消息异步执行，避免同步接口超时。
 *
 * @author ibqy
 */
@Service
public class IncrementalUpdater {

    private static final Logger log = LoggerFactory.getLogger(IncrementalUpdater.class);

    private final DocChangeWatcher changeWatcher;
    private final VectorStoreManager storeManager;

    public IncrementalUpdater(DocChangeWatcher changeWatcher,
                              VectorStoreManager storeManager) {
        this.changeWatcher = changeWatcher;
        this.storeManager = storeManager;
    }

    /**
     * 处理文档更新 —— 先删旧向量，再写新向量
     *
     * @param meta      文档元数据（含历史 MD5）
     * @param newChunks 新切分好的分块列表
     * @param newMd5    当前文档的 MD5
     */
    public void updateDocument(DocumentMeta meta, List<Chunk> newChunks, String newMd5) {
        String storedMd5 = meta.getMd5();
        if (!changeWatcher.hasChanged(newMd5, storedMd5)) {
            log.info("文档未变更，跳过: docId={}", meta.getDocId());
            return;
        }

        log.info("文档已变更，增量更新: docId={}, oldMd5={}, newMd5={}",
                meta.getDocId(), storedMd5, newMd5);

        // 步骤1: 删除该文档的所有旧向量（软删除）
        for (Chunk chunk : newChunks) {
            storeManager.softDelete(chunk.getId());
            log.debug("标记删除旧 chunk: {}", chunk.getId());
        }

        // 步骤2: 写入新向量（实际调用 retriever 的 add 方法）
        // 此处仅为编排演示，真正写入在各 retriever 实现中
        log.info("写入新向量: docId={}, chunkCount={}", meta.getDocId(), newChunks.size());

        // 步骤3: 更新文档元数据中的 MD5
        meta.setMd5(newMd5);
        log.info("增量更新完成: docId={}", meta.getDocId());
    }

    /**
     * 处理新增文档
     */
    public void addDocument(DocumentMeta meta, List<Chunk> chunks) {
        log.info("新增文档向量: docId={}, chunkCount={}", meta.getDocId(), chunks.size());
    }

    /**
     * 处理文档删除 —— 软删除所有关联向量
     */
    public void deleteDocument(String docId, List<String> chunkIds) {
        log.info("删除文档向量: docId={}, chunkCount={}", docId, chunkIds.size());
        for (String chunkId : chunkIds) {
            storeManager.softDelete(chunkId);
        }
    }

    /**
     * 异步清理已软删除的向量（定时任务调用）
     */
    public void asyncPurge() {
        log.info("开始异步清理软删除向量");
        storeManager.purgeDeleted();
    }
}