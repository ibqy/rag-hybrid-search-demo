package com.xb.rag.config;

import com.xb.rag.chunking.*;
import com.xb.rag.document.DocumentProcessor;
import com.xb.rag.document.cleaner.DocCleaner;
import com.xb.rag.document.parser.*;
import com.xb.rag.vectorstore.VectorStoreConfig;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 核心 Bean 配置 —— 将所有文档解析器、分块策略等
 * 手工 new 的组件注册为 Spring 管理的 Bean
 */
@Configuration
public class RagConfig {

    // ======== 文档解析器 ========

    @Bean
    public PdfParser pdfParser() {
        return new PdfParser();
    }

    @Bean
    public WordParser wordParser() {
        return new WordParser();
    }

    @Bean
    public MarkdownParser markdownParser() {
        return new MarkdownParser();
    }

    @Bean
    public OcrService ocrService() {
        return new OcrService();
    }

    @Bean
    public DocCleaner docCleaner() {
        return new DocCleaner();
    }

    @Bean
    public DocumentProcessor documentProcessor(List<DocumentParser> parsers, DocCleaner cleaner) {
        return new DocumentProcessor(parsers, cleaner);
    }

    // ======== 分块策略 ========

    @Bean
    public FixedSizeChunker fixedSizeChunker() {
        return new FixedSizeChunker();
    }

    @Bean
    public HierarchicalChunker hierarchicalChunker() {
        return new HierarchicalChunker();
    }

    @Bean
    public SemanticChunker semanticChunker(Optional<EmbeddingModel> embeddingModel) {
        return new SemanticChunker(embeddingModel);
    }

    @Bean
    public ChunkConfig chunkConfig() {
        return new ChunkConfig();
    }

    @Bean
    public ChunkingOrchestrator chunkingOrchestrator(FixedSizeChunker fixedSizeChunker,
                                                      HierarchicalChunker hierarchicalChunker,
                                                      SemanticChunker semanticChunker) {
        Map<String, ChunkStrategy> strategies = Map.of(
                "fixed", fixedSizeChunker,
                "hierarchical", hierarchicalChunker,
                "semantic", semanticChunker
        );
        return new ChunkingOrchestrator(strategies);
    }

    // ======== 向量库配置 ========

    @Bean
    public VectorStoreConfig vectorStoreConfig() {
        return new VectorStoreConfig(VectorStoreConfig.StoreType.PGVECTOR,
                VectorStoreConfig.IndexType.HNSW, 1536);
    }
}