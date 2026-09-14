package com.xb.springai.controller.demo08;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * demo08 配置：构建一个内存向量库（SimpleVectorStore）
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点——RAG 是什么？</b>RAG（Retrieval-Augmented Generation，检索增强生成）
 * 是"大模型 + 私有知识库"的经典方案：模型不知道你们公司的资料，但我们可以先把资料切分、
 * 转成向量存进向量库；用户提问时，先从向量库里检索最相关的几段，拼进提示词再让模型回答。
 * 这样模型就能基于"你自己的资料"来回答，而不必重新训练。</p>
 *
 * <p>三个核心动作对应的类：</p>
 * <ol>
 *     <li>切分 + 向量化：把文本切块，用 EmbeddingModel 把每块转成向量</li>
 *     <li>存储：存入 SimpleVectorStore（教学用内存向量库，生产可换成 PGVector等）</li>
 *     <li>检索：通过向量距离找到最相关的片段（demo08 控制器里演示）</li>
 * </ol>
 */
@Configuration
public class RagConfig {

    /**
     * 在应用启动时构建好向量库并存入门店资料。
     */
    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 1) 创建内存向量库（SimpleVectorStore 默认按余弦相似度检索）
        //    注意：Spring AI 2.x 中 builder() 需要直接传入 EmbeddingModel，
        //    因为向量库在构建时就要知道该用哪个模型把文本转成向量。
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel)
                .build();

        // 2) 读取"知识库资料"（classpath 下的纯文本，模拟一份私有知识库）
        List<Document> docs = new ArrayList<>();
        try {
            String raw = new ClassPathResource("kb/shop-intro.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            // 简单按段落("\n\n")切分，真实项目会交给专门的文本分割器
            for (String para : raw.split("\\n\\n")) {
                if (!para.isBlank()) {
                    docs.add(new Document(para.trim()));
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取知识库资料失败", e);
        }

        // 3) 向量化并写入向量库
        if (!docs.isEmpty()) {
            store.add(docs);
        }
        return store;
    }
}