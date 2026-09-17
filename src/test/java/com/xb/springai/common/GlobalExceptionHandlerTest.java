package com.xb.springai.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler 全局异常处理测试")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("ApiResponse 统一响应")
    class ApiResponseTests {

        @Test
        @DisplayName("ok 返回正确数据")
        void apiResponseOk() {
            ApiResponse<String> resp = ApiResponse.ok("hello");
            assertEquals(0, resp.code());
            assertEquals("ok", resp.message());
            assertEquals("hello", resp.data());
        }

        @Test
        @DisplayName("error 返回错误码和消息")
        void apiResponseError() {
            ApiResponse<Void> resp = ApiResponse.error(400, "bad request");
            assertEquals(400, resp.code());
            assertEquals("bad request", resp.message());
            assertNull(resp.data());
        }

        @Test
        @DisplayName("ok 允许 null 数据")
        void apiResponseOkWithNull() {
            ApiResponse<String> resp = ApiResponse.ok(null);
            assertEquals(0, resp.code());
            assertNull(resp.data());
        }
    }

    @Nested
    @DisplayName("异常处理方法")
    class HandlerTests {

        @Test
        @DisplayName("handleIllegalArgument 返回 400")
        void handleIllegalArgument() {
            var resp = handler.handleIllegalArgument(new IllegalArgumentException("参数不合法"));
            assertEquals(400, resp.getStatusCode().value());
            assertNotNull(resp.getBody());
            assertEquals(400, resp.getBody().code());
            assertEquals("参数不合法", resp.getBody().message());
        }

        @Test
        @DisplayName("handleUnknown 返回 500 并包含 requestId")
        void handleUnknown() {
            var resp = handler.handleUnknown(new RuntimeException("unexpected"));
            assertEquals(500, resp.getStatusCode().value());
            assertNotNull(resp.getBody());
            assertTrue(resp.getBody().message().contains("requestId="));
        }

        @Test
        @DisplayName("handleMissingParam 返回 400 并提示参数名")
        void handleMissingParam() {
            var ex = new MissingServletRequestParameterException("imageUrl", "GET");
            var resp = handler.handleMissingParam(ex);
            assertEquals(400, resp.getStatusCode().value());
            assertNotNull(resp.getBody());
            assertTrue(resp.getBody().message().contains("imageUrl"));
        }
    }
}
