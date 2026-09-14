package com.xb.rag.document;

import com.xb.rag.document.cleaner.DocCleaner;
import com.xb.rag.document.parser.DocumentParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 文档处理器 —— 编排解析、清洗全流程
 *
 * 职责：
 * 1. 根据扩展名识别文档类型
 * 2. 找到匹配的 DocumentParser
 * 3. 解析得到切片 + 元信息
 * 4. 计算文件 MD5
 * 5. 对每个切片文本执行清洗
 */
public class DocumentProcessor {

    private final List<DocumentParser> parsers;
    private final DocCleaner cleaner;

    public DocumentProcessor(List<DocumentParser> parsers, DocCleaner cleaner) {
        this.parsers = parsers;
        this.cleaner = cleaner;
    }

    public DocumentProcessor(List<DocumentParser> parsers) {
        this(parsers, new DocCleaner());
    }

    /**
     * 处理文档入口
     * @param filePath 文件绝对路径
     * @param options  解析选项（可空）
     * @return ParseResult
     */
    public ParseResult processDocument(String filePath, Map<String, String> options) {
        if (options == null) options = Collections.emptyMap();

        Path path = Path.of(filePath);
        String fileName = path.getFileName().toString();

        // 1. 识别文档类型
        DocType docType = detectFileType(fileName);
        if (docType == DocType.UNKNOWN) {
            DocumentMeta meta = new DocumentMeta(UUID.randomUUID().toString(), fileName, filePath, DocType.UNKNOWN);
            return new ParseResult(meta, Collections.emptyList(), false, "不支持的文件类型: " + fileName);
        }

        // 2. 查找解析器
        DocumentParser parser = findParser(docType);
        if (parser == null) {
            DocumentMeta meta = new DocumentMeta(UUID.randomUUID().toString(), fileName, filePath, docType);
            return new ParseResult(meta, Collections.emptyList(), false, "未找到解析器: " + docType);
        }

        // 3. 解析
        ParseResult result;
        try (InputStream input = Files.newInputStream(path)) {
            result = parser.parse(input, fileName, options);
        } catch (IOException e) {
            DocumentMeta meta = new DocumentMeta(UUID.randomUUID().toString(), fileName, filePath, docType);
            return new ParseResult(meta, Collections.emptyList(), false, "文件读取失败: " + e.getMessage());
        }

        if (!result.isSuccess()) {
            return result;
        }

        // 4. 填充元信息
        DocumentMeta meta = result.getDocMeta();
        meta.setSourcePath(filePath);
        meta.setDocId(UUID.randomUUID().toString());

        // 计算文件大小和 MD5
        try {
            meta.setFileSize(Files.size(path));
            meta.setMd5(computeMd5(path));
        } catch (IOException e) {
            meta.setMd5("");
        }

        // 5. 清洗每个切片的文本
        List<DocSegment> cleanedSegments = new ArrayList<>();
        for (DocSegment seg : result.getSegments()) {
            seg.setDocId(meta.getDocId());
            String cleaned = DocCleaner.clean(seg.getContent());
            seg.setContent(cleaned);
            if (!cleaned.isBlank()) {
                cleanedSegments.add(seg);
            }
        }

        return new ParseResult(meta, cleanedSegments, true);
    }

    // 根据扩展名映射 DocType
    private DocType detectFileType(String fileName) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        return switch (ext) {
            case "pdf" -> DocType.PDF;
            case "docx" -> DocType.DOCX;
            case "pptx" -> DocType.PPTX;
            case "md", "markdown" -> DocType.MARKDOWN;
            case "png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff" -> DocType.IMAGE;
            case "txt" -> DocType.PLAIN_TEXT;
            case "html", "htm" -> DocType.HTML;
            default -> DocType.UNKNOWN;
        };
    }

    // 根据 DocType 找匹配的解析器
    private DocumentParser findParser(DocType type) {
        for (DocumentParser p : parsers) {
            if (p.supports(type)) return p;
        }
        return null;
    }

    // 计算文件 MD5
    private String computeMd5(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = Files.readAllBytes(path);
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 算法不可用", e);
        }
    }
}