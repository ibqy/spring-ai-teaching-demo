package com.xb.springai.controller.demo07;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo07：系统角色（System Message）—— 给模型"立人设"
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：一次对话里有两类消息：</p>
 * <ul>
 *     <li><b>系统消息</b>（system）：放在最前，用来设定模型的角色、语气、规则、边界等"底层人设"</li>
 *     <li><b>用户消息</b>（user）：用户本轮的实际输入</li>
 * </ul>
 *
 * <p>好的系统提示词能显著提升回答质量。注意：越是前置、越是靠近"人设"的指令，模型越容易遵守。
 * 本例用 {@code system(s -> s.text(...).param(...))} 动态传入角色名，人设也能模板化。</p>
 */
@RestController
@RequestMapping("/api/demo07")
public class Demo07SystemRoleController {

    private final ChatClient chatClient;

    public Demo07SystemRoleController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                // 也可在构建 ChatClient 时设置默认系统消息：defaultSystem(...)
                .build();
    }

    /**
     * GET /api/demo07/advice?role=Java高级工程师&question=如何学习Spring AI
     * 让模型以指定身份回答问题。
     */
    @GetMapping("/advice")
    public String advice(
            @RequestParam(defaultValue = "Java高级工程师") String role,
            @RequestParam(defaultValue = "如何高效学习 Spring AI") String question) {
        return this.chatClient.prompt()
                .system(s -> s
                        // 系统消息：设定角色与回答要求（也可用变量 {role} 模板化）
                        .text("你是一位资深的 {role}，回答问题时请专业、简洁、条理清晰，并给出可落地的建议。")
                        .param("role", role))
                .user(question)   // 用户消息：本次要解决的问题
                .call()
                .content();
    }
}