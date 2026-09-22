package com.xb.springai.controller.demo02;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * demo02：流式对话 —— 逐字/逐句返回 Stream
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：demo01 的 {@code call()} 是"全部生成完再一次性返回"，体验像等电梯。
 * 而 {@code stream()} 是"边生成边返回"，体验像接水龙头，用户看到文字一点点"长出来"。
 * 这是如今所有 AI 应用（如 ChatGPT）打字机效果的实现原理。</p>
 *
 * <ul>
 *     <li>返回类型：{@link Flux}{@code <String>}，是响应式编程里的"多个元素的数据流"</li>
 *     <li>{@code produces=TEXT_EVENT_STREAM_VALUE}：告诉浏览器以 Server-Sent Events 方式接收</li>
 *     <li>流式基于 Project Reactor（WebFlux 的核心响应式库）</li>
 * </ul>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo02")
public class Demo02StreamingChatController {

    private final ChatClient chatClient;

    public Demo02StreamingChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GET /api/demo02/chat-stream?message=写一首短诗
     * 流式返回。浏览器/前端拿到的是持续到达的多个文本片段。
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam(defaultValue = "写一首关于编程的短诗") String message) {
        return this.chatClient.prompt()
                .user(message)   // 用户消息
                .stream()        // 流式调用（与 call 相反）
                .content();      // 取出流式的文本片段序列
    }
}