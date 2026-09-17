package com.xb.rag.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * 全局异常处理器（WebFlux 兼容）
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 *
 * <p><b>教学知识点</b>：WebFlux 项目中，{@code @RestControllerAdvice} 同样适用，
 * 可以集中处理所有 Controller 抛出的异常。本项目的特殊之处：</p>
 * <ul>
 *   <li>{@link ResponseStatusException} — WebFlux 推荐的异常类型，自带 HTTP 状态码</li>
 *   <li>{@link IllegalArgumentException} — RagController 的 record 校验抛出</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException e) {
        log.warn("ResponseStatusException: {} {}", e.getStatusCode(), e.getReason());
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error(e.getStatusCode().value(), e.getReason()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.error("未知异常 [{}]: {}", requestId, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "系统异常，requestId=" + requestId));
    }
}
