package com.xb.springai.controller.demo11;

import jakarta.validation.constraints.NotBlank;

/**
 * demo11：组合实战 —— 请求体 DTO
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p>为了演示"更真实的接口"，demo11 放弃 GET 传参，改用 POST + JSON 请求体，
 * 更贴近真实后端 API。</p>
 *
 * @param conversationId 会话 ID（用于记忆隔离）
 * @param message        本轮用户消息
 *
 * @author ibqy
 */
public record Demo11AskRequest(
        String conversationId,
        @NotBlank(message = "message 不能为空")
        String message) {
}