package com.xb.rag.vectorstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文档变更检测器 —— 通过 MD5 指纹判断文档是否已变更
 *
 * 只有 MD5 发生变化时，才触发增量更新流程。
 * MD5 不变直接跳过处理，避免重复解析和向量化。
 *
 * @author ibqy
 */
@Service
public class DocChangeWatcher {

    private static final Logger log = LoggerFactory.getLogger(DocChangeWatcher.class);

    /**
     * 计算文档内容的 MD5 指纹
     *
     * @param content 文档原始内容（字节数组）
     * @return 32 位十六进制 MD5 字符串
     */
    public String computeMd5(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 算法不可用", e);
            throw new RuntimeException("MD5 not available", e);
        }
    }

    /**
     * 计算文档文本内容的 MD5 指纹
     */
    public String computeMd5(String content) {
        if (content == null) return "";
        return computeMd5(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 判断文档是否已变更
     *
     * @param currentMd5 当前计算出的 MD5
     * @param storedMd5  向量库中存储的历史 MD5
     * @return true = 文档已变更，需要重新处理
     */
    public boolean hasChanged(String currentMd5, String storedMd5) {
        if (storedMd5 == null || storedMd5.isEmpty()) {
            return true; // 新文档，未入库过
        }
        return !currentMd5.equals(storedMd5);
    }
}