package com.xb.springai.controller.demo04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo04：提示词模板 —— 用 {变量} 复用同一套提示词
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：写死整句提示词不好维护。Spring AI 支持在提示词中用 {@code {xxx}}
 * 声明占位符，再通过 {@code .param("xxx", 值)} 传入变量，实现"一套模板、多次填充"，
 * 效果与 JDBC 的 PreparedStatement（{@code ?} 占位符）思想一致。</p>
 */
@RestController
@RequestMapping("/api/demo04")
public class Demo04PromptTemplateController {

    private final ChatClient chatClient;

    public Demo04PromptTemplateController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GET /api/demo04/poem?topic=春天
     * 同一个模板，换个主题即可生成不同主题的诗。
     */
    @GetMapping("/poem")
    public String poem(@RequestParam(defaultValue = "春天") String topic) {
        return this.chatClient.prompt()
                .user(u -> u
                        // {topic} 是变量占位符，运行时会替换成 param 传入的值
                        .text("请你写一首关于 {topic} 的五言绝句，要求押韵、有意境。")
                        .param("topic", topic))
                .call()
                .content();
    }
}