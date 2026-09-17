package com.xb.rag.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器 + 统一响应封装 单元测试
 *
 * <p>作者：xb | 日期：2026-09-17</p>
 */
@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("ApiResponse 统一响应")
    class ApiResponseTest {

        @Test
        @DisplayName("ok — 正常返回")
        void apiResponseOk() {
            ApiResponse<String> resp = ApiResponse.ok("data");
            assertEquals(0, resp.code());
            assertEquals("ok", resp.message());
            assertEquals("data", resp.data());
        }

        @Test
        @DisplayName("error — 错误返回")
        void apiResponseError() {
            ApiResponse<Void> resp = ApiResponse.error(500, "boom");
            assertEquals(500, resp.code());
            assertEquals("boom", resp.message());
            assertNull(resp.data());
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("ResponseStatusException → 对应状态码")
        void handleResponseStatus() {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "samples 不能为空");
            ResponseEntity<ApiResponse<Void>> resp = handler.handleResponseStatus(ex);
            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals(400, resp.getBody().code());
            assertTrue(resp.getBody().message().contains("samples"));
        }

        @Test
        @DisplayName("IllegalArgumentException → 400")
        void handleIllegalArgument() {
            ResponseEntity<ApiResponse<Void>> resp =
                    handler.handleIllegalArgument(new IllegalArgumentException("question 不能为空"));
            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertTrue(resp.getBody().message().contains("question"));
        }

        @Test
        @DisplayName("未知异常 → 500 + requestId")
        void handleUnknown() {
            ResponseEntity<ApiResponse<Void>> resp =
                    handler.handleUnknown(new RuntimeException("unexpected"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertTrue(resp.getBody().message().contains("requestId="));
        }
    }
}
