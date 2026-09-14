package com.xb.springai.controller.demo06;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo06：会话记忆 —— 让模型记住"上文"
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：大模型是无状态的，每次提问都是全新的对话，它记不住你上一句说了什么。
 * Spring AI 通过在调用链上挂一个"记忆顾问（Advisor）"来解决：它会自动把同一会话的
 * 历史消息取出、拼接回传给模型，从而实现多轮上下文。</p>
 *
 * <p>关键点：</p>
 * <ul>
 *     <li>{@code ChatMemory}：Spring AI 自动配置好的内存会话存储（默认 MessageWindowChatMemory，保留最近 20 条）</li>
 *     <li>{@code MessageChatMemoryAdvisor.builder(chatMemory).build()}：把记忆接入 ChatClient 的默认调用链</li>
 *     <li>{@code ChatMemory.CONVERSATION_ID}：用会话 ID 区分不同用户/不同会话，彼此互不干扰</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/demo06")
public class Demo06ChatMemoryController {

    private final ChatClient chatClient;

    /**
     * 构造时把记忆顾问挂到 ChatClient 的默认调用链上。
     * 之后所有由该 client 发起的请求，都会带上此前同一会话的历史。
     */
    public Demo06ChatMemoryController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    /**
     * GET /api/demo06/chat?conversationId=xb&message=我的名字是小北
     * 先用同一 conversationId 说"我的名字是小北"，再问"我叫什么"，模型能回忆起来。
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String conversationId,
                       @RequestParam(defaultValue = "你好") String message) {
        return this.chatClient.prompt()
                .user(message)
                // 关键：指定这句消息属于哪个会话，记忆按此隔离
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}