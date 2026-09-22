package com.xb.springai.controller.demo12;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo12：使用自定义 Advisor —— 给每次调用加日志
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>进阶点</b>：这个控制器演示如何把自定义 {@link Demo12LoggingAdvisor} 挂到
 * ChatClient 上。之后每次要模型地回答，日志里都会打印"收到的请求内容"和"调用耗时"，
 * 你可以观察控制台日志来验证 Advisor 是否生效。</p>
 *
 * <p><b>学习目标</b>：理解了 Advisor 后，你就能自由扩展 Spring AI——
 * 例如做一个"敏感词拦截顾问""成本统计顾问""结果缓存顾问"，都是同一个套路。</p>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo12")
public class Demo12AdvisorController {

    private final ChatClient chatClient;

    /**
     * 注入自定义 InfoLogger，并通过 defaultAdvisors 挂到调用链上，
     * 等价于给"每一次模型调用"织入日志切面。
     */
    public Demo12AdvisorController(ChatClient.Builder chatClientBuilder, Demo12LoggingAdvisor loggingAdvisor) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(loggingAdvisor)   // 关键：注册自定义顾问
                .build();
    }

    /**
     * GET /api/demo12/chat?message=你好
     * 每次调用，控制台都会输出 Demo12LoggingAdvisor 的日志。
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "你好，请介绍一下你自己") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}