package com.xb.springai.controller.demo11;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo11：组合实战课代表 —— 完整的多轮智能客服
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>为什么是这个 Demo</b>：前面 8 个 Demo 各自讲了单一能力。真实业务里，AI 应用
 * 需要把这些能力<mark>组合</mark>起来。这个 Demo 把已学到的知识点串成一个可用的客服机器人：</p>
 * <ul>
 *   <li><b>会话记忆</b>（demo06）：→ 多轮上下文，记住用户聊过什么</li>
 *   <li><b>系统角色</b>（demo07）：→ 立客服人设，限定语气与边界</li>
 *   <li><b>RAG 知识库</b>（demo08）：→ 让它能回答"店铺资料"里的问题（营业时间等）</li>
 *   <li><b>函数调用</b>（demo05/09）：→ 让它能查实时售后政策</li>
 *   <li><b>POST + JSON</b>：→ 更贴近真实后端接口设计</li>
 * </ul>
 *
 * <p>这正是"一个 Spring AI 实战项目"的雏形：一个控制器 + 一个记忆顾问 + 一个向量库
 * + 一个工具 Bean，就组装出一个像模像样的智能客服。</p>
 *
 * <p>注：demo13 新增了第二个向量库 Bean（etlVectorStore），因此这里用
 * {@code @Qualifier("vectorStore")} 明确注入 demo08 构建的客服知识库。</p>
 */
@RestController
@RequestMapping("/api/demo11")
public class Demo11CustomerServiceController {

    private final ChatClient chatClient;

    /**
     * 构造时把所有能力一次性注入：
     * ChatMemory 记忆 → 挂成默认顾问；VectorStore 知识库 → 保存在成员变量供检索；
     * AfterSalesTool 工具 → 作为默认工具注册。
     */
    public Demo11CustomerServiceController(ChatClient.Builder chatClientBuilder,
                                           ChatMemory chatMemory,
                                           @Qualifier("vectorStore") VectorStore vectorStore,
                                           AfterSalesTool afterSalesTool) {
        // 注意：这里把 VectorStore 传入生成 RAG 检索提示词，Behind the scenes 依然复用 demo08 的向量库
        this.chatClient = chatClientBuilder
                // 1) 记忆顾问：让模型记住同一会话的上下文
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // 2) 默认工具：售后政策查询
                .defaultTools(afterSalesTool)
                // 3) 全局系统角色：客服人设
                .defaultSystem(s -> s
                        .text("你是'小北优选咖啡店'的智能客服'小北'。请用友好、简洁的语气回答。" +
                              "回答个人信息类问题请客气；只能依据提供的店铺资料与工具结果回答，" +
                              "资料与工具未提到的内容不要编造。"))
                .build();
        // 保留向量库引用供方法内检索（RAG）
        this.vectorStore = vectorStore;
    }

    private final VectorStore vectorStore;

    /**
     * POST /api/demo11/ask   {"conversationId":"xb-1","message":"你们几点营业？"}
     *
     * 内部流程：
     * 1) 先用向量库检索与问题最相关的资料片段（RAG）
     * 2) 把资料拼进系统上下文，再走记忆+工具+角色 的完整调用链
     */
    @PostMapping("/ask")
    public String ask(@RequestBody Demo11AskRequest req) {
        // -- 1. 检索知识库（RAG，等价于 demo08 的步骤） --
        List<Document> related = vectorStore.similaritySearch(
                SearchRequest.builder().query(req.message()).topK(3).build());
        String context = related.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // -- 2. 组装一次完整调用 --（记忆/角色/工具均由 ChatClient 默认配置生效）
        return this.chatClient.prompt()
                // RAG：把检索到的资料作为"参考"注入本轮
                .user(u -> u
                        .text("""
                              请基于以下店铺资料并结合你的记忆回答用户。
                              资料中未提到的，请不要编造。

                              【店铺资料】：
                              {context}

                              【用户消息】：{message}
                              """)
                        .param("context", context.isEmpty() ? "（无相关资料）" : context)
                        .param("message", req.message()))
                // 记忆隔离：同一 conversationId 属于同一次会话
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, req.conversationId()))
                .call()
                .content();
    }
}