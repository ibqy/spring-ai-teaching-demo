package com.xb.springai.controller.demo13;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * demo13 配置：用标准 ETL 管道把 PDF / Word 等文档接入知识库
 *
 * <p>作者：ibqy | 日期：2026-09-14</p>
 *
 * <p><b>教学知识点——Spring AI 2.0 ETL 管道是什么？</b>
 * ETL（Extract 抽取 → Transform 转换 → Load 加载）是把原始文档变成向量库中
 * 可检索数据的三步流水线，对应三个接口：</p>
 * <ol>
 *     <li>DocumentReader（抽取）：把各种格式的文档读成统一的 List&lt;Document&gt;</li>
 *     <li>DocumentTransformer（转换）：切分、清洗文本，控制每块大小</li>
 *     <li>DocumentWriter（加载）：把处理好的文档写入向量库（VectorStore 即实现之一）</li>
 * </ol>
 *
 * <p>对比 demo08：demo08 是手写 {@code split("\n\n")} 手工切分 txt，只适合最简单的场景；
 * demo13 用 {@code reader.read() → splitter.split() → store.write()} 的标准管道，
 * 一个 {@link TikaDocumentReader} 就能解析 PDF、Word（doc/docx）、PPT、HTML、Markdown
 * 等 1000+ 种格式，切分也更专业（按 token 保留语义块）。</p>
 */
@Configuration
public class EtlConfig {

    /**
     * 构建一个独立的 ETL 向量库（与 demo08 的内存向量库互不干扰）。
     * 应用启动时自动执行：读取 kb/ 下所有文档 → 按 token 切分 → 向量化写入。
     */
    @Bean
    public SimpleVectorStore etlVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel)
                .build();

        // -- 1) Extract 抽取：Tika 万能读取器解析 classpath:kb/ 下的全部文档 --
        //     教学示例内置 kb/shop-policy.md；生产环境把 PDF / DOCX 丢进该目录即可，
        //     或用 FileSystemResource 指向外部文件夹。一个 Reader 通吃多种格式。
        List<Document> docs = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:kb/*");
            for (Resource resource : resources) {
                docs.addAll(new TikaDocumentReader(resource).read());
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取知识库文档失败", e);
        }

        // -- 2) Transform 转换：按 token 切分成适合检索的小块（默认每块 800 token）--
        List<Document> chunks = new TokenTextSplitter().split(docs);

        // -- 3) Load 加载：写入向量库（VectorStore 实现了 DocumentWriter 接口）--
        if (!chunks.isEmpty()) {
            store.write(chunks);
        }
        return store;
    }
}