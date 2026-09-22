package com.xb.rag.controller;

import com.xb.rag.chunking.Chunk;
import com.xb.rag.chunking.ChunkConfig;
import com.xb.rag.chunking.ChunkingOrchestrator;
import com.xb.rag.document.DocumentMeta;
import com.xb.rag.document.DocumentProcessor;
import com.xb.rag.document.ParseResult;
import com.xb.rag.vectorstore.DocChangeWatcher;
import com.xb.rag.vectorstore.IncrementalUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档管理接口 —— 上传、解析、切片、增量更新
 *
 * POST /api/docs/upload   上传并解析文档
 * POST /api/docs/update   增量更新已有文档
 * DELETE /api/docs/{docId} 删除文档
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/docs")
public class DocController {

    private static final Logger log = LoggerFactory.getLogger(DocController.class);

    private final DocumentProcessor documentProcessor;
    private final ChunkingOrchestrator chunkingOrchestrator;
    private final ChunkConfig chunkConfig;
    private final DocChangeWatcher changeWatcher;
    private final IncrementalUpdater incrementalUpdater;

    public DocController(DocumentProcessor documentProcessor,
                         ChunkingOrchestrator chunkingOrchestrator,
                         ChunkConfig chunkConfig,
                         DocChangeWatcher changeWatcher,
                         IncrementalUpdater incrementalUpdater) {
        this.documentProcessor = documentProcessor;
        this.chunkingOrchestrator = chunkingOrchestrator;
        this.chunkConfig = chunkConfig;
        this.changeWatcher = changeWatcher;
        this.incrementalUpdater = incrementalUpdater;
    }

    /**
     * 上传并解析文档 —— 接收文件，执行解析→切片→元数据生成流程
     *
     * @param file     上传的文档文件（支持 PDF/DOCX/MD）
     * @param tenantId 租户标识，默认 "default"
     * @return 包含 docId、文件名、切片数和 MD5 的响应
     */
    @PostMapping("/upload")
    public Mono<DocUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "tenantId", defaultValue = "default") String tenantId) {
        return Mono.fromCallable(() -> {
            // 1. 读文件
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            if (fileName == null) fileName = "unknown";

            // 2. 构造元数据
            DocumentMeta meta = new DocumentMeta();
            meta.setDocId(UUID.randomUUID().toString());
            meta.setDocName(fileName);
            meta.setSourcePath(fileName);
            meta.setTenantId(tenantId);
            meta.setUpdatedAt(LocalDateTime.now().toString());
            meta.setMd5(changeWatcher.computeMd5(bytes));

            // 3. 解析文档
            ParseResult parseResult = documentProcessor.process(fileName, bytes, meta);

            // 4. 切片
            List<Chunk> chunks = chunkingOrchestrator.chunk(parseResult, chunkConfig);

            log.info("文档上传解析完成: docId={}, chunks={}", meta.getDocId(), chunks.size());
            return new DocUploadResponse(meta.getDocId(), meta.getDocName(),
                    chunks.size(), meta.getMd5());
        });
    }

    /**
     * 增量更新文档 —— 上传新版本，对比 MD5 后仅更新变化部分
     *
     * @param file     新版文档文件
     * @param docId    待更新的文档 ID
     * @param tenantId 租户标识，默认 "default"
     * @return 包含 docId、更新状态、切片数和 MD5 的响应
     */
    @PostMapping("/update")
    public Mono<Map<String, Object>> update(@RequestParam("file") MultipartFile file,
                                             @RequestParam("docId") String docId,
                                             @RequestParam(value = "tenantId", defaultValue = "default") String tenantId) {
        return Mono.fromCallable(() -> {
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            if (fileName == null) fileName = "unknown";

            String newMd5 = changeWatcher.computeMd5(bytes);

            DocumentMeta meta = new DocumentMeta();
            meta.setDocId(docId);
            meta.setDocName(fileName);
            meta.setTenantId(tenantId);
            meta.setUpdatedAt(LocalDateTime.now().toString());
            meta.setMd5(newMd5);

            ParseResult parseResult = documentProcessor.process(fileName, bytes, meta);
            List<Chunk> chunks = chunkingOrchestrator.chunk(parseResult, chunkConfig);
            incrementalUpdater.updateDocument(meta, chunks, newMd5);

            return Map.of("docId", docId, "status", "updated",
                    "chunks", chunks.size(), "md5", newMd5);
        });
    }

    public record DocUploadResponse(String docId, String docName, int chunkCount, String md5) {}
}