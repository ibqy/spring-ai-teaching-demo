package com.xb.springai.controller.demo03;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo03：结构化输出 —— 让模型输出 Java 对象
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：使用 {@code .call().entity(Demo03ActorFilm.class)}，
 * Spring AI 会提示模型返回符合该结构定义的 JSON，并自动反序列化成对象。
 * 返回具体的 Java 类型，比返回一串文本更便于上层直接使用。</p>
 *
 * <p><b>进阶</b>：{@code entity(类型, spec -> spec.useProviderStructuredOutput().validateSchema())}
 * 可以把 JSON 结构以 API 级约束方式下发到模型，并自动校验失败重试。</p>
 *
 * <p><b>两个端点</b>：
 * <ul>
 *   <li>{@code /actor} — 简单场景：2 字段的演员-电影 record</li>
 *   <li>{@code /review} — 复杂场景：5 字段的代码审查结果，结合多变量 PromptTemplate</li>
 * </ul>
 * </p>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo03")
public class Demo03StructuredOutputController {

    private final ChatClient chatClient;

    public Demo03StructuredOutputController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GET /api/demo03/actor?actor=周星驰
     * 返回一个 {"actor":"周星驰","movies":[...]} 的 JSON 对象。
     */
    @GetMapping("/actor")
    public Demo03ActorFilm actor(@RequestParam(defaultValue = "周星驰") String actor) {
        return this.chatClient.prompt()
                .user(u -> u
                        // 用提示词模板描述要输出的结构
                        .text("请为演员 {actor} 自动生成他的 2 部代表作。")
                        .param("actor", actor))
                .call()
                .entity(Demo03ActorFilm.class);   // 关键：把回答直接映射为 Java 对象
    }

    /**
     * GET /api/demo03/review?language=java&code=public class Foo { ... }
     * 让模型充当代码审查员，返回结构化的审查结果（评分、问题、建议、总结）。
     *
     * <p>演示同一个 {@code .entity()} 机制可以映射任意复杂度的 Java 对象，
     * 结合 PromptTemplate 的 {@code {变量}} 注入代码和语言参数。</p>
     */
    @GetMapping("/review")
    public Demo03CodeReviewResult review(
            @RequestParam(defaultValue = "public class Hello { public static void main(String[] args) { System.out.println(\"Hello\"); } }") String code,
            @RequestParam(defaultValue = "java") String language) {
        return this.chatClient.prompt()
                .user(u -> u
                        .text("请以资深工程师身份审查以下 {language} 代码。\n"
                                + "返回：分数(0-100)、问题列表、改进建议、总结。\n\n"
                                + "```{language}\n{code}\n```")
                        .param("language", language)
                        .param("code", code))
                .call()
                .entity(Demo03CodeReviewResult.class);
    }
}
