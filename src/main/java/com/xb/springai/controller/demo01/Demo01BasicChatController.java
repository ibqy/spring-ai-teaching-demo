package com.xb.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo01：最简单的对话 —— 认识 ChatClient
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：ChatClient 是 Spring AI 面向开发者最重要的门面对象，提供流式的 API
 * （Fluent API），三步完成一次对话：</p>
 * <ol>
 *     <li>{@code prompt().user(消息)}：构造一次请求，user() 把你输入的话作为"用户消息"</li>
 *     <li>{@code .call()}：同步调用大模型（阻塞等待返回结果）</li>
 *     <li>{@code .content()}：取出模型返回的文本内容</li>
 * </ol>
 *
 * <p><b>对比参考项目</b>：就像 MyBatis-Plus 用 Lambda 链式查询简化操作一样，
 * ChatClient 把对话描述成一条链式调用，非常直观。</p>
 */
@RestController
@RequestMapping("/api/demo01")
public class Demo01BasicChatController {

    /**
     * ChatClient.Builder 是 Spring AI 自动配置给我们的"构建器"（原型 Bean）。
     * 它已经帮我准备好了默认的 ChatModel 等组件，我们只管 build() 出一个 ChatClient 使用。
     */
    private final ChatClient chatClient;

    public Demo01BasicChatController(ChatClient.Builder chatClientBuilder) {
        // 用构建器生成一个 ChatClient 实例
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GET /api/demo01/chat?message=你好
     * 演示最基础的"一问一答"。
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "用一句话介绍什么是 Spring AI") String message) {
        // 1) 把用户消息交给模型     2) 同步调用     3) 取出文本回答
        return this.chatClient.prompt()
                .user(message)   // 用户消息
                .call()          // 同步调用
                .content();      // 取出回答文本
    }
}