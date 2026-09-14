package com.xb.rag.chunking;

import com.xb.rag.document.DocSegment;
import com.xb.rag.document.ParseResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 分块编排器 —— 根据配置选择合适的分块策略，
 * 并对生成的分块进行合法性校验。
 */
public class ChunkingOrchestrator {

    /** 策略名称到策略实现的映射 */
    private final Map<String, ChunkStrategy> strategies;

    public ChunkingOrchestrator(Map<String, ChunkStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 对文档切片执行分块。
     *
     * @param segments 文档切片列表
     * @param config   分块配置
     * @return 校验通过的分块列表
     */
    public List<Chunk> chunk(List<DocSegment> segments, ChunkConfig config) {
        if (segments == null || segments.isEmpty()) {
            return new ArrayList<>();
        }
        if (config == null || config.getStrategy() == null) {
            throw new IllegalArgumentException("分块配置或策略名称不能为空");
        }

        ChunkStrategy strategy = strategies.get(config.getStrategy());
        if (strategy == null) {
            throw new IllegalArgumentException("未知的分块策略: " + config.getStrategy()
                    + "，可用策略: " + strategies.keySet());
        }

        List<Chunk> chunks = strategy.chunk(segments, config);
        return validate(chunks, config.getMinChunkSize());
    }

    /**
     * 校验单个分块是否合法。
     *
     * @param chunk       待校验的分块
     * @param minChunkSize 最小分块大小
     * @return true 表示合法
     */
    public boolean validate(Chunk chunk, int minChunkSize) {
        if (chunk == null) {
            return false;
        }
        String content = chunk.getContent();
        // 内容为空或全空白
        if (content == null || content.isBlank()) {
            return false;
        }
        // 内容太短
        if (content.length() < minChunkSize) {
            return false;
        }
        return true;
    }

    /**
     * 过滤掉不合法的分块。
     *
     * @param chunks      分块列表
     * @param minChunkSize 最小分块大小
     * @return 过滤后的合法分块列表
     */
    public List<Chunk> validate(List<Chunk> chunks, int minChunkSize) {
        List<Chunk> valid = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (validate(chunk, minChunkSize)) {
                valid.add(chunk);
            }
        }
        return valid;
    }

    /**
     * 对 ParseResult 执行分块（Controller 便利方法）
     *
     * @param parseResult 文档解析结果
     * @param config      分块配置
     * @return 校验通过的分块列表
     */
    public List<Chunk> chunk(ParseResult parseResult, ChunkConfig config) {
        if (parseResult == null) {
            return new ArrayList<>();
        }
        return chunk(parseResult.getSegments(), config);
    }

    /**
     * 获取已注册的策略映射（不可修改视图）。
     */
    public Map<String, ChunkStrategy> getStrategies() {
        return strategies;
    }
}