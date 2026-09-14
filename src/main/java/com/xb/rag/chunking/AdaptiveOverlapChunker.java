package com.xb.rag.chunking;

import com.xb.rag.document.DocSegment;
import java.util.ArrayList;
import java.util.List;

/**
 * 自适应重叠分块器 —— 装饰器模式，包装另一个 ChunkStrategy，
 * 在分块完成后根据内容长度和类型动态调整重叠大小。
 *
 * 规则：
 * - 短块（< 100 字符）→ 重叠为 0
 * - 长块（> 1000 字符）→ 最大重叠（min(config.chunkOverlap, 256)）
 * - 表格/代码块 → 重叠为 0
 */
public class AdaptiveOverlapChunker implements ChunkStrategy {

    private static final int SHORT_THRESHOLD = 100;
    private static final int LONG_THRESHOLD = 1000;
    private static final int MAX_OVERLAP = 256;

    private final ChunkStrategy delegate;

    public AdaptiveOverlapChunker(ChunkStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<Chunk> chunk(List<DocSegment> segments, ChunkConfig config) {
        // 先由委托策略执行分块
        List<Chunk> rawChunks = delegate.chunk(segments, config);
        if (rawChunks == null || rawChunks.isEmpty()) {
            return rawChunks;
        }

        List<Chunk> adjusted = new ArrayList<>();

        for (int i = 0; i < rawChunks.size(); i++) {
            Chunk chunk = rawChunks.get(i);
            String content = chunk.getContent();
            String contentType = chunk.getContentType();
            int contentLen = content != null ? content.length() : 0;

            // 计算当前块应使用的重叠大小
            int effectiveOverlap = computeOverlap(contentLen, contentType, config.getChunkOverlap());

            // 将重叠信息写入 metadata
            chunk.getMetadata().put("overlap", effectiveOverlap);
            chunk.getMetadata().put("adaptive", true);

            // 如果重叠为 0，且不是第一个块，裁剪掉前一个块的重叠部分
            if (effectiveOverlap == 0 && i > 0) {
                Chunk prevChunk = adjusted.get(adjusted.size() - 1);
                // 确保前一块不与当前块重叠
                prevChunk.getMetadata().put("overlap", 0);
            }

            adjusted.add(chunk);
        }

        return adjusted;
    }

    @Override
    public String strategyName() {
        return "adaptive";
    }

    /**
     * 获取被装饰的委托策略。
     */
    public ChunkStrategy getDelegate() {
        return delegate;
    }

    /**
     * 根据内容长度和类型计算应使用的重叠大小。
     */
    private int computeOverlap(int contentLen, String contentType, int configuredOverlap) {
        // 表格和代码块重叠为 0
        if ("table".equals(contentType) || "code".equals(contentType)) {
            return 0;
        }

        // 短块重叠为 0
        if (contentLen < SHORT_THRESHOLD) {
            return 0;
        }

        // 长块使用最大重叠
        if (contentLen > LONG_THRESHOLD) {
            return Math.min(configuredOverlap, MAX_OVERLAP);
        }

        // 中等长度使用配置值
        return configuredOverlap;
    }
}