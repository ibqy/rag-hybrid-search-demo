package com.xb.rag.document.parser;

import com.xb.rag.document.DocType;
import com.xb.rag.document.ParseResult;

import java.io.InputStream;
import java.util.Map;

/**
 * 文档解析器接口 —— 每种文档类型实现一个
 *
 * @author ibqy
 */
public interface DocumentParser {

    /**
     * 判断本解析器是否支持指定的文档类型
     * @param type 文档类型枚举
     * @return true 表示支持
     */
    boolean supports(DocType type);

    /**
     * 解析输入流，返回结构化的文档切片结果
     * @param input    文件输入流
     * @param fileName 文件名（用于元信息和类型识别）
     * @param options  解析选项（如 OCR 语言等，可为空）
     * @return 解析结果，包含元数据和切片列表
     */
    ParseResult parse(InputStream input, String fileName, Map<String, String> options);

    /**
     * 返回本解析器处理的文档类型
     * @return 文档类型枚举值
     */
    DocType supportedType();
}