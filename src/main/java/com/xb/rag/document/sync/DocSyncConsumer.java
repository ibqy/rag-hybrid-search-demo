package com.xb.rag.document.sync;

import com.xb.rag.chunking.Chunk;
import com.xb.rag.chunking.ChunkConfig;
import com.xb.rag.chunking.ChunkingOrchestrator;
import com.xb.rag.document.DocumentMeta;
import com.xb.rag.document.DocumentProcessor;
import com.xb.rag.document.ParseResult;
import com.xb.rag.vectorstore.IncrementalUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文档同步消费者 —— 从 Kafka 异步消费文档变更消息
 *
 * 生产环境：文档上传 API 只做"存文件 + 发消息",
 * 耗时操作（解析、切片、向量化）由本消费者异步执行，
 * 避免同步接口超时。
 *
 * @author ibqy
 */
@Component
public class DocSyncConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocSyncConsumer.class);

    private final DocumentProcessor documentProcessor;
    private final ChunkingOrchestrator chunkingOrchestrator;
    private final ChunkConfig chunkConfig;
    private final IncrementalUpdater incrementalUpdater;

    public DocSyncConsumer(DocumentProcessor documentProcessor,
                           ChunkingOrchestrator chunkingOrchestrator,
                           ChunkConfig chunkConfig,
                           IncrementalUpdater incrementalUpdater) {
        this.documentProcessor = documentProcessor;
        this.chunkingOrchestrator = chunkingOrchestrator;
        this.chunkConfig = chunkConfig;
        this.incrementalUpdater = incrementalUpdater;
    }

    /**
     * 消费文档同步消息
     * topic = rag-doc-sync，group-id 已在 application.yml 配置
     */
    @KafkaListener(topics = "${kafka.topic.doc-sync:rag-doc-sync}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consume(DocSyncMessage message) {
        log.info("消费到文档同步消息: action={}, docId={}, file={}",
                message.getAction(), message.getDocId(), message.getDocName());

        try {
            switch (message.getAction()) {
                case ADD, UPDATE -> handleAddOrUpdate(message);
                case DELETE -> handleDelete(message);
            }
        } catch (Exception e) {
            log.error("文档同步处理失败: docId={}, error={}",
                    message.getDocId(), e.getMessage(), e);
        }
    }

    private void handleAddOrUpdate(DocSyncMessage msg) throws IOException {
        // 从存储路径读取文件
        Path filePath = Path.of(msg.getStoredPath());
        if (!Files.exists(filePath)) {
            log.error("文件不存在: {}", msg.getStoredPath());
            return;
        }
        byte[] content = Files.readAllBytes(filePath);

        // 构造元数据
        DocumentMeta meta = new DocumentMeta(msg.getDocId(), msg.getDocName(),
                msg.getStoredPath(), null);
        meta.setTenantId(msg.getTenantId());
        meta.setMd5(msg.getMd5());

        // 解析 + 切片
        ParseResult parseResult = documentProcessor.process(msg.getDocName(), content, meta);
        List<Chunk> chunks = chunkingOrchestrator.chunk(parseResult.getSegments(), chunkConfig);

        // 增量写入向量库
        if (msg.getAction() == DocSyncMessage.Action.ADD) {
            incrementalUpdater.addDocument(meta, chunks);
        } else {
            incrementalUpdater.updateDocument(meta, chunks, msg.getMd5());
        }

        log.info("文档异步处理完成: docId={}, chunks={}", msg.getDocId(), chunks.size());
    }

    private void handleDelete(DocSyncMessage msg) {
        incrementalUpdater.deleteDocument(msg.getDocId(), List.of());
        log.info("文档异步删除完成: docId={}", msg.getDocId());
    }
}