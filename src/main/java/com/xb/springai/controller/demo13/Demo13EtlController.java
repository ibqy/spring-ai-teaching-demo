package com.xb.springai.controller.demo13;

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
 * demo13：ETL 文档管道 —— 让模型回答"PDF / Word 里的内容"
 *
 * <p>作者：ibqy | 日期：2026-09-14</p>
 *
 * <p><b>教学知识点</b>：ETL 与 RAG 的分工。demo08 演示的是"已有向量库"的检索问答；
 * demo13 补上"向量库里的数据从哪来"这一环——用 ETL 管道把 PDF、Word 等文档自动
 * 变成向量，之后检索回答的链路与 demo08 完全一致（similaritySearch + 拼资料 + 生成）。</p>
 *
 * <p>因为项目里现在有两个向量库 Bean（demo08 的 vectorStore 与 demo13 的
 * etlVectorStore），注入时必须用 {@code @Qualifier} 指名道姓。这也是生产项目的
 * 常见做法：一个应用按业务域拆多个知识库，各自注入互不干扰。</p>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo13")
public class Demo13EtlController {

    private final ChatClient chatClient;
    private final VectorStore etlVectorStore;

    public Demo13EtlController(ChatClient.Builder chatClientBuilder,
                               @Qualifier("etlVectorStore") VectorStore etlVectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.etlVectorStore = etlVectorStore;
    }

    /**
     * GET /api/demo13/ask?question=会员积分怎么算？
     * 模型只会基于 kb/ 目录下文档（内置 shop-policy.md）里的内容回答。
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "会员积分规则是什么？") String question) {
        // 1) 检索与问题最相关的 3 段（数据源来自 ETL 管道解析出的文档块）
        List<Document> related = etlVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build()
        );

        // 2) 把检索结果拼成"参考资料"文本
        String context = related.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // 3) 资料 + 问题 一起交给模型，并明确要求"只能依据资料回答"
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