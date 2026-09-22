package com.xb.rag.document;

/**
 * 文档类型枚举 —— 定义系统支持解析的文档格式
 *
 * <p>支持 PDF、Word、PPT、Markdown、图片等常见格式，UNKNOWN 作为兜底。</p>
 *
 * @author ibqy
 */
public enum DocType {
    PDF, DOCX, PPTX, MARKDOWN, IMAGE, PLAIN_TEXT, HTML, UNKNOWN
}