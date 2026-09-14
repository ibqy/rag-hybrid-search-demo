package com.xb.rag;

import com.xb.rag.chunking.*;
import com.xb.rag.document.DocSegment;
import com.xb.rag.document.DocType;
import com.xb.rag.document.DocumentMeta;
import com.xb.rag.document.parser.MarkdownParser;
import com.xb.rag.retrieval.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG 核心链路集成测试 —— 不依赖外部服务，纯内存验证
 *
 * 测试链路：Markdown解析 → 层级切片 → 固定切分 → RRF融合
 * 验证分块策略的正确性和检索结果的基本逻辑
 */
class RagIntegrationTest {

    private static MarkdownParser markdownParser;
    private static HierarchicalChunker hierarchicalChunker;
    private static FixedSizeChunker fixedSizeChunker;

    @BeforeAll
    static void setup() {
        markdownParser = new MarkdownParser();
        hierarchicalChunker = new HierarchicalChunker();
        fixedSizeChunker = new FixedSizeChunker();
    }

    @Test
    void testMarkdownParsing() {
        String md = """
                # 用户手册
                
                系统登录说明。
                
                ## 登录步骤
                
                1. 打开浏览器
                2. 输入网址
                3. 点击登录
                
                ## 常见问题
                
                Q: 密码忘记怎么办？
                
                ```java
                // 代码块应该完整保留
                public void reset() {}
                ```
                
                | 功能 | 说明 |
                | --- | --- |
                | 登录 | 使用账号密码 |
                """;

        DocumentMeta meta = new DocumentMeta("doc-001", "test.md", "/test.md", DocType.MARKDOWN);
        var result = markdownParser.parse(
                new ByteArrayInputStream(md.getBytes(StandardCharsets.UTF_8)),
                "test.md", Map.of());

        assertTrue(result.isSuccess());
        List<DocSegment> segments = result.getSegments();

        // 应该解析出多个章节，包含标题、正文、代码块、表格
        assertFalse(segments.isEmpty(), "解析结果不应为空");

        // 验证代码块完整
        boolean hasCode = segments.stream().anyMatch(s -> "code".equals(s.getContentType()));
        assertTrue(hasCode, "应包含代码块");

        // 验证表格保留
        boolean hasTable = segments.stream().anyMatch(s -> "table".equals(s.getContentType()));
        assertTrue(hasTable, "应包含表格");

        // 验证标题层级
        boolean hasHeading = segments.stream().anyMatch(s -> s.getHeadingLevel() > 0);
        assertTrue(hasHeading, "应包含标题");

        System.out.println("=== Markdown 解析测试通过 ===");
        System.out.println("切片数: " + segments.size());
        segments.forEach(s -> System.out.printf("  [%s] Lv%d: %s...%n",
                s.getContentType(), s.getHeadingLevel(),
                s.getContent().substring(0, Math.min(30, s.getContent().length()))));
    }

    @Test
    void testFixedSizeChunking() {
        // 构建一批切片
        List<DocSegment> segments = List.of(
                new DocSegment("seg-1", "doc-001", "这是第一段文本内容。", "text"),
                new DocSegment("seg-2", "doc-001", "这是第二段文本内容，包含更多信息。", "text"),
                new DocSegment("seg-3", "doc-001", "| col1 | col2 |\n| --- | --- |\n| a | b |", "table"),
                new DocSegment("seg-4", "doc-001", "这是第三段。", "text")
        );

        ChunkConfig config = new ChunkConfig()
                .setStrategy("fixed")
                .setChunkSize(50)
                .setChunkOverlap(0)
                .setMinChunkSize(5);

        List<Chunk> chunks = fixedSizeChunker.chunk(segments, config);

        assertFalse(chunks.isEmpty(), "分块结果不应为空");

        // 验证表格独立成块
        boolean tableChunk = chunks.stream()
                .anyMatch(c -> "table".equals(c.getContentType()));
        assertTrue(tableChunk, "表格应独立成块");

        System.out.println("=== 固定大小分块测试通过 ===");
        System.out.println("分块数: " + chunks.size());
        chunks.forEach(c -> System.out.printf("  [%s] 长度=%d: %s%n",
                c.getContentType(), c.getContent().length(),
                c.getContent().substring(0, Math.min(20, c.getContent().length()))));
    }

    @Test
    void testHierarchicalChunking() {
        // 构造带标题层级的数据
        DocSegment seg1 = new DocSegment("s1", "doc-002", "第一章介绍", "text");
        seg1.setSectionTitle("第一章");
        seg1.setHeadingLevel(1);
        DocSegment seg2 = new DocSegment("s2", "doc-002", "第一节内容", "text");
        seg2.setSectionTitle("第一章");
        DocSegment seg3 = new DocSegment("s3", "doc-002", "第二节内容", "text");
        seg3.setSectionTitle("第一章");
        DocSegment seg4 = new DocSegment("s4", "doc-002", "第二章介绍", "text");
        seg4.setSectionTitle("第二章");
        seg4.setHeadingLevel(1);

        List<DocSegment> segments = List.of(seg1, seg2, seg3, seg4);
        ChunkConfig config = new ChunkConfig()
                .setStrategy("hierarchical")
                .setChunkSize(200)
                .setMinChunkSize(3);

        List<Chunk> chunks = hierarchicalChunker.chunk(segments, config);

        // 应该按章节分块：第一章 3 个 segment 在一起，第二章 1 个
        assertEquals(2, chunks.size(), "应生成 2 个分块（按章节）");
        assertEquals("第一章", chunks.get(0).getSectionTitle());
        assertEquals("第二章", chunks.get(1).getSectionTitle());

        System.out.println("=== 层级分块测试通过 ===");
        chunks.forEach(c -> System.out.printf("  [%s] 段落数=%d%n",
                c.getSectionTitle(), c.getSegmentIds().size()));
    }

    @Test
    void testRrfFusion() {
        RrfFusion fusion = new RrfFusion(60);

        // 模拟向量结果（top-3）
        List<SearchResult> vectorResults = List.of(
                new SearchResult("chunk-a", "doc-1", "内容A").setScore(0.85).setRank(1).setSource("vector"),
                new SearchResult("chunk-b", "doc-1", "内容B").setScore(0.72).setRank(2).setSource("vector"),
                new SearchResult("chunk-c", "doc-1", "内容C").setScore(0.60).setRank(3).setSource("vector")
        );

        // 模拟 BM25 结果（chunk-b 和 chunk-d）
        List<SearchResult> bm25Results = List.of(
                new SearchResult("chunk-b", "doc-1", "内容B").setScore(0.90).setRank(1).setSource("bm25"),
                new SearchResult("chunk-d", "doc-1", "内容D").setScore(0.80).setRank(2).setSource("bm25"),
                new SearchResult("chunk-e", "doc-1", "内容E").setScore(0.70).setRank(3).setSource("bm25")
        );

        // 融合，取 top-3
        List<SearchResult> fused = fusion.fuse(vectorResults, bm25Results, 3);

        // chunk-b 在两路都排名靠前，应在最前面
        assertEquals(3, fused.size(), "应返回 top-3");
        assertEquals("chunk-b", fused.get(0).getChunkId(), "chunk-b 应排第一（两路都有）");

        System.out.println("=== RRF 融合测试通过 ===");
        fused.forEach(r -> System.out.printf("  rank=%d id=%s score=%.4f%n",
                r.getRank(), r.getChunkId(), r.getScore()));
    }
}