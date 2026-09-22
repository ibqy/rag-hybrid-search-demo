package com.xb.rag.chunking;

import com.xb.rag.document.DocSegment;
import java.util.List;

/**
 * 分块策略接口 —— 定义了如何将文档切片（DocSegment）组装成语义块（Chunk）
 *
 * @author ibqy
 */
public interface ChunkStrategy {

    /**
     * 对文档切片列表执行分块，返回分块结果
     *
     * @param segments 文档切片列表
     * @param config   分块配置
     * @return 分块列表
     */
    List<Chunk> chunk(List<DocSegment> segments, ChunkConfig config);

    /**
     * 返回策略名称，例如 "hierarchical"、"semantic"、"fixed"
     */
    String strategyName();
}