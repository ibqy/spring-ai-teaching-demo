package com.xb.springai.controller.demo08;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo08：RAG 检索增强生成 —— 让模型回答"你自己的知识库"
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：RAG 的完整链路：</p>
 * <ol>
 *     <li>把用户问题转成向量，到向量库做最近邻检索（{@code similaritySearch}）</li>
 *     <li>取出最相关的 topK 段文本作为"参考资料"</li>
 *     <li>把资料 + 原问题一起拼进提示词，让模型"先读资料再回答"</li>
 * </ol>
 *
 * <p>对比 demo05：函数调用是"模型主动去查实时数据"，RAG 是"应用主动给模型喂资料"，
 * 两者经常结合使用，是搭建企业级 AI 应用的基石。</p>
 *
 * <p>注：demo13 新增了第二个向量库 Bean（etlVectorStore），因此这里用
 * {@code @Qualifier("vectorStore")} 明确注入 demo08 自己构建的那个向量库。</p>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo08")
public class Demo08RagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public Demo08RagController(ChatClient.Builder chatClientBuilder,
                               @Qualifier("vectorStore") VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * GET /api/demo08/ask?question=店铺几点关门？
     * 模型只会基于 shop-intro.txt 里的资料回答。
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "这家店营业时间是什么？") String question) {
        // 1) 到向量库里检索与问题最相关的 3 段内容
        List<Document> related = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build()
        );

        // 2) 把检索结果拼成"参考资料"文本
        String context = related.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 3) 把"资料 + 问题"一起交给模型，并明确要求"只能依据资料回答"
        return this.chatClient.prompt()
                .user(u -> u
                        .text("""
                              请仅根据下面提供的资料回答用户问题。
                              如果资料里没有相关内容，请直接回答"资料中未提及"。

                              【资料】：
                              {context}

                              【问题】：{question}
                              """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .content();
    }
}