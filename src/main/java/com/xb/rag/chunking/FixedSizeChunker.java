package com.xb.rag.chunking;

import com.xb.rag.document.DocSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 固定大小分块器 —— 按字符数简单切分，支持重叠。
 * 表格/代码块不会被切分到两个块中。
 *
 * @author ibqy
 */
public class FixedSizeChunker implements ChunkStrategy {

    @Override
    public List<Chunk> chunk(List<DocSegment> segments, ChunkConfig config) {
        List<Chunk> result = new ArrayList<>();
        if (segments == null || segments.isEmpty()) {
            return result;
        }

        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        // 按 docId 分组
        List<DocSegment> docSegments = new ArrayList<>(segments);
        String docId = docSegments.get(0).getDocId();

        StringBuilder buffer = new StringBuilder();
        List<String> segIds = new ArrayList<>();
        String currentContentType = null;
        List<Integer> pageNums = new ArrayList<>();

        int chunkIndex = 0;

        for (DocSegment seg : docSegments) {
            String segContent = seg.getContent();
            String segType = seg.getContentType();

            // 表格/代码块独立处理：不与其他内容混合
            if ("table".equals(segType) || "code".equals(segType)) {
                // 将当前 buffer 中的内容先落盘
                if (!buffer.isEmpty()) {
                    result.add(buildChunk(docId, buffer.toString(), segIds, currentContentType,
                            pageNums, seg.getSectionTitle(), seg.getHeadingLevel(), chunkIndex++));
                    buffer = new StringBuilder();
                    segIds = new ArrayList<>();
                    pageNums = new ArrayList<>();
                    currentContentType = null;
                }

                // 表格/代码块作为一个独立分块
                List<String> singleSegIds = new ArrayList<>();
                singleSegIds.add(seg.getId());
                List<Integer> singlePages = new ArrayList<>();
                singlePages.add(seg.getPageNum());
                result.add(buildChunk(docId, segContent, singleSegIds, segType,
                        singlePages, seg.getSectionTitle(), seg.getHeadingLevel(), chunkIndex++));
                continue;
            }

            // 普通文本：累积到 buffer，达到 chunkSize 则切分
            if (buffer.length() + segContent.length() > chunkSize && !buffer.isEmpty()) {
                result.add(buildChunk(docId, buffer.toString(), segIds, currentContentType,
                        pageNums, seg.getSectionTitle(), seg.getHeadingLevel(), chunkIndex++));

                // 重叠处理：保留末尾 overlap 字符所属的 segments
                String overlapText = buffer.length() > overlap
                        ? buffer.substring(buffer.length() - overlap)
                        : buffer.toString();
                buffer = new StringBuilder(overlapText);
                // 保留所有 segIds（简化处理），实际可按内容匹配优化
            }

            if (buffer.isEmpty()) {
                currentContentType = segType;
            }
            buffer.append(segContent);
            segIds.add(seg.getId());
            pageNums.add(seg.getPageNum());
        }

        // 处理最后一个 buffer
        if (!buffer.isEmpty()) {
            DocSegment lastSeg = docSegments.get(docSegments.size() - 1);
            result.add(buildChunk(docId, buffer.toString(), segIds, currentContentType,
                    pageNums, lastSeg.getSectionTitle(), lastSeg.getHeadingLevel(), chunkIndex++));
        }

        return result;
    }

    @Override
    public String strategyName() {
        return "fixed";
    }

    private Chunk buildChunk(String docId, String content, List<String> segIds,
                             String contentType, List<Integer> pageNums,
                             String sectionTitle, int headingLevel, int chunkIndex) {
        int avgPage = pageNums.stream().mapToInt(Integer::intValue).sum() / Math.max(pageNums.size(), 1);
        Chunk chunk = new Chunk();
        chunk.setId(UUID.randomUUID().toString());
        chunk.setDocId(docId);
        chunk.setContent(content);
        chunk.setSegmentIds(new ArrayList<>(segIds));
        chunk.setPageNum(avgPage);
        chunk.setSectionTitle(sectionTitle);
        chunk.setHeadingLevel(headingLevel);
        chunk.setContentType(contentType != null ? contentType : "text");
        chunk.setChunkIndex(chunkIndex);
        return chunk;
    }
}