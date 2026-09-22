package com.xb.rag.chunking;

import com.xb.rag.document.DocSegment;
import org.springframework.ai.embedding.EmbeddingModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 语义分块器 —— 利用嵌入向量计算句子组之间的余弦相似度，
 * 在语义转折处切分。当 EmbeddingModel 不可用时回退到固定大小分块。
 *
 * @author ibqy
 */
public class SemanticChunker implements ChunkStrategy {

    /** 余弦相似度阈值，低于此值认为语义不一致，在此处切分 */
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private final Optional<EmbeddingModel> embeddingModel;

    public SemanticChunker(EmbeddingModel embeddingModel) {
        this.embeddingModel = Optional.ofNullable(embeddingModel);
    }

    public SemanticChunker(Optional<EmbeddingModel> embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Chunk> chunk(List<DocSegment> segments, ChunkConfig config) {
        if (segments == null || segments.isEmpty()) {
            return new ArrayList<>();
        }

        // 如果嵌入模型不可用，回退到固定大小分块
        if (embeddingModel.isEmpty()) {
            return fallbackFixedChunk(segments, config);
        }

        EmbeddingModel model = embeddingModel.get();
        int chunkSize = config.getChunkSize();
        List<Chunk> result = new ArrayList<>();

        // 按 docId 分组
        String docId = segments.get(0).getDocId();

        // 分句子组：每组累积不超过 chunkSize
        List<List<DocSegment>> groups = groupSegments(segments, chunkSize);

        // 为每组计算嵌入向量
        List<float[]> embeddings = new ArrayList<>();
        for (List<DocSegment> group : groups) {
            String text = groupText(group);
            try {
                float[] vec = model.embed(text);
                embeddings.add(vec);
            } catch (Exception e) {
                // 单个组嵌入失败时使用零向量
                embeddings.add(new float[0]);
            }
        }

        // 按余弦相似度切分
        List<List<DocSegment>> semanticGroups = new ArrayList<>();
        List<DocSegment> currentGroup = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            List<DocSegment> group = groups.get(i);

            if (currentGroup.isEmpty()) {
                currentGroup.addAll(group);
                continue;
            }

            // 计算当前组与上一组的相似度
            float[] prevVec = embeddings.get(i - 1);
            float[] currVec = embeddings.get(i);

            double similarity = cosineSimilarity(prevVec, currVec);

            if (similarity < SIMILARITY_THRESHOLD) {
                // 语义转折，切分
                semanticGroups.add(new ArrayList<>(currentGroup));
                currentGroup = new ArrayList<>();
            }

            currentGroup.addAll(group);
        }

        // 最后一组
        if (!currentGroup.isEmpty()) {
            semanticGroups.add(currentGroup);
        }

        // 构建 Chunk
        int chunkIndex = 0;
        for (List<DocSegment> sg : semanticGroups) {
            result.add(buildChunk(docId, sg, chunkIndex++));
        }

        return result;
    }

    @Override
    public String strategyName() {
        return "semantic";
    }

    /**
     * 将切片分成若干小组，每组累积字符数不超过 chunkSize。
     */
    private List<List<DocSegment>> groupSegments(List<DocSegment> segments, int chunkSize) {
        List<List<DocSegment>> groups = new ArrayList<>();
        List<DocSegment> current = new ArrayList<>();
        int currentLen = 0;

        for (DocSegment seg : segments) {
            // 表格/代码块独立成组
            if ("table".equals(seg.getContentType()) || "code".equals(seg.getContentType())) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                    currentLen = 0;
                }
                List<DocSegment> single = new ArrayList<>();
                single.add(seg);
                groups.add(single);
                continue;
            }

            int segLen = seg.getContent().length();
            if (currentLen + segLen > chunkSize && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
                currentLen = 0;
            }

            current.add(seg);
            currentLen += segLen;
        }

        if (!current.isEmpty()) {
            groups.add(current);
        }

        return groups;
    }

    /**
     * 将一组切片合并为纯文本。
     */
    private String groupText(List<DocSegment> segs) {
        StringBuilder sb = new StringBuilder();
        for (DocSegment seg : segs) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(seg.getContent());
        }
        return sb.toString();
    }

    /**
     * 计算两个向量之间的余弦相似度。
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 1.0; // 无法计算时返回 1.0（不切分）
        }

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 1.0 : dot / denom;
    }

    /**
     * 嵌入模型不可用时的回退策略：固定大小分块。
     */
    private List<Chunk> fallbackFixedChunk(List<DocSegment> segments, ChunkConfig config) {
        FixedSizeChunker fallback = new FixedSizeChunker();
        return fallback.chunk(segments, config);
    }

    /**
     * 从一组切片构建分块。
     */
    private Chunk buildChunk(String docId, List<DocSegment> segs, int chunkIndex) {
        StringBuilder content = new StringBuilder();
        List<String> segIds = new ArrayList<>();
        List<Integer> pageNums = new ArrayList<>();
        String sectionTitle = null;
        int headingLevel = 0;
        String contentType = "text";

        for (DocSegment seg : segs) {
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(seg.getContent());
            segIds.add(seg.getId());
            pageNums.add(seg.getPageNum());
            if (seg.getSectionTitle() != null) {
                sectionTitle = seg.getSectionTitle();
                headingLevel = seg.getHeadingLevel();
            }
            String ct = seg.getContentType();
            if ("table".equals(ct) || "code".equals(ct)) {
                contentType = ct;
            }
        }

        int avgPage = pageNums.stream().mapToInt(Integer::intValue).sum() / Math.max(pageNums.size(), 1);

        Chunk chunk = new Chunk();
        chunk.setId(UUID.randomUUID().toString());
        chunk.setDocId(docId);
        chunk.setContent(content.toString());
        chunk.setSegmentIds(segIds);
        chunk.setPageNum(avgPage);
        chunk.setSectionTitle(sectionTitle != null ? sectionTitle : "");
        chunk.setHeadingLevel(headingLevel);
        chunk.setContentType(contentType);
        chunk.setChunkIndex(chunkIndex);
        return chunk;
    }
}