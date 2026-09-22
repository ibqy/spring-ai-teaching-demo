package com.xb.springai.common;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler - 全局异常处理器
 *
 * <p>利用 {@code @RestControllerAdvice} 实现全局异常拦截：当任何控制器抛出未捕获的异常时，
 * Spring 会自动路由到对应的 {@code @ExceptionHandler} 方法，统一返回 {@link ApiResponse}
 * 格式的错误响应，避免前端收到裸的 500 堆栈信息。</p>
 *
 * <p>教学要点：异常处理的优先级从高到低依次为精确类型匹配、父类匹配、
 * 最终的 {@code Exception.class} 兜底，兜底方法会生成随机 requestId 方便排查日志。</p>
 *
 * @author ibqy
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理缺少必要请求参数的异常，返回 400 并提示缺失的参数名。
     *
     * @param ex 缺少参数异常
     * @return 包含参数名的错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("缺少请求参数: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "缺少必要参数: " + ex.getParameterName()));
    }

    /**
     * 处理 Bean 校验失败（@Valid / @Validated）的异常，返回 400 并提示首个校验失败的字段。
     *
     * @param ex 校验异常
     * @return 包含字段校验信息的错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String fieldError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", fieldError);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, fieldError));
    }

    /**
     * 处理业务逻辑中抛出的非法参数异常，返回 400 并携带异常信息。
     *
     * @param ex 非法参数异常
     * @return 包含错误描述的错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    /**
     * 兜底处理所有未被上层捕获的未知异常，返回 500。
     *
     * <p>生成一个随机 requestId 并写入响应，方便开发者在日志中定位对应的完整堆栈，
     * 同时避免把内部实现细节（类名、行号）暴露给客户端。</p>
     *
     * @param ex 未知异常
     * @return 包含 requestId 的错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        log.error("未知错误 [{}]: {}", requestId, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误 [requestId=" + requestId + "]"));
    }
}
