package com.xb.springai.common;

/**
 * ApiResponse - 统一 API 响应包装器
 *
 * <p>所有控制器接口的返回值都通过这个 record 统一封装，保证前端拿到的 JSON
 * 格式始终是 {@code {code, message, data}} 三件套，方便统一处理成功/失败逻辑。</p>
 *
 * <p>使用 Java record 实现，天然不可变、自带 equals/hashCode/toString，
 * 比传统 POJO 更简洁，适合做纯粹的数据载体（DTO）。</p>
 *
 * @author ibqy
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
