package com.xb.rag.chunking;

import com.xb.rag.document.DocSegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 层级分块器 —— 按文档 → 章节层级组装块。
 * 当章节内容超过 chunkSize 时，在该章节内继续拆分，但保留标题边界。
 * 表格/代码段保持完整，不会跨块。
 *
 * @author ibqy
 */
public class HierarchicalChunker implements ChunkStrategy {

    @Override
    public List<Chunk> chunk(List<DocSegment> segments, ChunkConfig config) {
        List<Chunk> result = new ArrayList<>();
        if (segments == null || segments.isEmpty()) {
            return result;
        }

        int chunkSize = config.getChunkSize();

        // 1. 按 docId 分组
        Map<String, List<DocSegment>> byDoc = segments.stream()
                .collect(Collectors.groupingBy(DocSegment::getDocId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<DocSegment>> docEntry : byDoc.entrySet()) {
            String docId = docEntry.getKey();
            List<DocSegment> docSegs = docEntry.getValue();

            // 2. 按 sectionTitle 分组（保留顺序）
            Map<String, List<DocSegment>> bySection = groupBySection(docSegs);

            int chunkIndex = 0;
            for (Map.Entry<String, List<DocSegment>> sectionEntry : bySection.entrySet()) {
                String sectionTitle = sectionEntry.getKey();
                List<DocSegment> sectionSegs = sectionEntry.getValue();

                // 获取该章节的标题级别
                int headingLevel = sectionSegs.get(0).getHeadingLevel();

                // 3. 估算章节总长度
                int totalLen = sectionSegs.stream().mapToInt(s -> s.getContent().length()).sum();

                // 如果章节长度在 chunkSize 内，直接作为一个块
                if (totalLen <= chunkSize) {
                    Chunk chunk = buildSectionChunk(docId, sectionSegs, sectionTitle, headingLevel, chunkIndex++);
                    result.add(chunk);
                    continue;
                }

                // 4. 章节超长，需要在该章节内继续拆分
                List<Chunk> subChunks = splitWithinSection(docId, sectionSegs, sectionTitle,
                        headingLevel, config, chunkIndex);
                chunkIndex += subChunks.size();
                result.addAll(subChunks);
            }
        }

        return result;
    }

    @Override
    public String strategyName() {
        return "hierarchical";
    }

    /**
     * 按连续相同的 sectionTitle 分组。
     * null / 空标题的段落归入前一个章节。
     */
    private Map<String, List<DocSegment>> groupBySection(List<DocSegment> segments) {
        Map<String, List<DocSegment>> result = new LinkedHashMap<>();
        String currentSection = "_no_section_";

        for (DocSegment seg : segments) {
            String title = seg.getSectionTitle();
            if (title != null && !title.isBlank()) {
                currentSection = title;
            }
            result.computeIfAbsent(currentSection, k -> new ArrayList<>()).add(seg);
        }

        return result;
    }

    /**
     * 在单个章节内拆分，保留标题边界，表格/代码块保持完整。
     */
    private List<Chunk> splitWithinSection(String docId, List<DocSegment> segs,
                                           String sectionTitle, int headingLevel,
                                           ChunkConfig config, int startIndex) {
        List<Chunk> result = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int chunkIndex = startIndex;

        List<DocSegment> currentBatch = new ArrayList<>();
        int currentLen = 0;

        for (DocSegment seg : segs) {
            int segLen = seg.getContent().length();
            boolean isSpecial = "table".equals(seg.getContentType()) || "code".equals(seg.getContentType());

            // 特殊类型：独立成块
            if (isSpecial) {
                // 先落盘当前 batch
                if (!currentBatch.isEmpty()) {
                    result.add(buildSectionChunk(docId, currentBatch, sectionTitle, headingLevel, chunkIndex++));
                    currentBatch = new ArrayList<>();
                    currentLen = 0;
                }
                // 独立块
                List<DocSegment> single = new ArrayList<>();
                single.add(seg);
                result.add(buildSectionChunk(docId, single, sectionTitle, headingLevel, chunkIndex++));
                continue;
            }

            // 普通文本：累积到超过 chunkSize 就切分
            if (currentLen + segLen > chunkSize && !currentBatch.isEmpty()) {
                result.add(buildSectionChunk(docId, currentBatch, sectionTitle, headingLevel, chunkIndex++));
                currentBatch = new ArrayList<>();
                currentLen = 0;
            }

            currentBatch.add(seg);
            currentLen += segLen;
        }

        // 最后一批
        if (!currentBatch.isEmpty()) {
            result.add(buildSectionChunk(docId, currentBatch, sectionTitle, headingLevel, chunkIndex++));
        }

        return result;
    }

    /**
     * 用一组切片构建一个分块，自动聚合内容、页面编号等信息。
     */
    private Chunk buildSectionChunk(String docId, List<DocSegment> segs,
                                    String sectionTitle, int headingLevel, int chunkIndex) {
        StringBuilder content = new StringBuilder();
        List<String> segIds = new ArrayList<>();
        List<Integer> pageNums = new ArrayList<>();
        String contentType = "text";

        for (DocSegment seg : segs) {
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(seg.getContent());
            segIds.add(seg.getId());
            pageNums.add(seg.getPageNum());

            // 取最高优先级的内容类型
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
        chunk.setSectionTitle(sectionTitle);
        chunk.setHeadingLevel(headingLevel);
        chunk.setContentType(contentType);
        chunk.setChunkIndex(chunkIndex);
        return chunk;
    }
}