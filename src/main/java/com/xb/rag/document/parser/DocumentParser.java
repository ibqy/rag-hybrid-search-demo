package com.xb.rag.document.parser;

import com.xb.rag.document.DocType;
import com.xb.rag.document.ParseResult;

import java.io.InputStream;
import java.util.Map;

/**
 * 文档解析器接口 —— 每种文档类型实现一个
 */
public interface DocumentParser {

    // 是否支持该文档类型
    boolean supports(DocType type);

    // 解析输入流，返回结构化的切片结果
    ParseResult parse(InputStream input, String fileName, Map<String, String> options);

    // 本解析器处理的文档类型
    DocType supportedType();
}