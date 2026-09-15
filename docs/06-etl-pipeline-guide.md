# 06 · ETL 文档管道

> **前置 Demo**：demo08（理解 RAG 基础）  
> **涉及 Demo**：demo13  
> **目标**：掌握 TikaDocumentReader → TokenTextSplitter → VectorStore 的完整 ETL 实战。

---

## 1. 什么是 ETL 管道？

**ETL（Extract → Transform → Load）** 是把原始文档变成向量库中可检索数据的三步流水线。它回答了 RAG 最前置的问题——**向量库里的数据从哪来**。

```
PDF/Word/Markdown/HTML 文件
        │
        ▼
┌─────────────────────────────┐
│ Extract（抽取）              │
│ TikaDocumentReader          │
│ 一个 Reader 解析 1000+ 格式  │
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│ Transform（转换）            │
│ TokenTextSplitter           │
│ 按 token 智能切分，保留语义   │
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│ Load（加载）                 │
│ VectorStore.write()         │
│ 写入向量库，支持相似度检索    │
└─────────────────────────────┘
```

对应 Spring AI 的三个核心接口：

| 阶段 | Spring AI 接口 | 本项目实现 |
|------|---------------|-----------|
| Extract | `DocumentReader` | `TikaDocumentReader`（解析 PDF/Word/HTML/Markdown 等） |
| Transform | `DocumentTransformer` | `TokenTextSplitter`（按 token 语义切分） |
| Load | `DocumentWriter` | `SimpleVectorStore`（内存向量库） |

---

## 2. demo08 vs demo13：两种数据入库方式

| 维度 | demo08 | demo13 |
|------|--------|--------|
| 文档格式 | 仅 txt | PDF / Word / HTML / Markdown / PPT 等 |
| 切分方式 | 手工 `split("\\n\\n")` | `TokenTextSplitter` 按 token 切分 |
| 文件扫描 | 硬编码单文件路径 | `PathMatchingResourcePatternResolver` 扫描目录 |
| 文档读取 | `ClassPathResource + String` | `TikaDocumentReader.read()` |
| 适用场景 | 简单教学 | **生产级文档接入** |

---

## 3. 完整 ETL 实现（demo13）

```java
@Configuration
public class EtlConfig {

    @Bean
    public SimpleVectorStore etlVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        // ============ 1. Extract 抽取 ============
        // Tika 万能读取器解析 classpath:kb/ 下的全部文档
        // 内置 kb/shop-policy.md；丢 PDF/DOCX 进去即可自动解析
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

        // ============ 2. Transform 转换 ============
        // 按 token 切分成适合检索的小块（默认每块 800 token）
        List<Document> chunks = new TokenTextSplitter().split(docs);

        // ============ 3. Load 加载 ============
        // VectorStore 实现了 DocumentWriter 接口
        if (!chunks.isEmpty()) {
            store.write(chunks);
        }
        return store;
    }
}
```

---

## 4. TikaDocumentReader —— 万能文档解析

基于 Apache Tika，一个 Reader 通吃 1000+ 种文件格式：

| 格式 | 说明 |
|------|------|
| PDF | 文本、表格、元数据 |
| DOC / DOCX | Word 文档 |
| PPT / PPTX | 演示文稿 |
| HTML / XML | 网页与标记语言 |
| Markdown | `.md` 文件（教学示例内置） |
| Plain Text | `.txt` 文件 |
| EPUB | 电子书 |

**不需**为每种格式引入不同的 Reader，一个 `TikaDocumentReader` 全部搞定。

### 使用方式

```java
// 从 classpath 读取
new TikaDocumentReader(new ClassPathResource("kb/shop-policy.md")).read();

// 从文件系统读取（生产环境常用）
new TikaDocumentReader(new FileSystemResource("/data/docs/report.pdf")).read();

// 从 URL 读取
new TikaDocumentReader(new UrlResource("https://example.com/doc.html")).read();
```

---

## 5. TokenTextSplitter —— 智能文本切分

### 为什么需要切分？

大模型一次能处理的上下文有限（token 窗口），向量检索也需要小块文本才能精准匹配。直接把一整本书丢进向量库是无效的。

### TokenTextSplitter 的切分策略

```java
// 默认参数：每块 800 token，重叠 200 token
List<Document> chunks = new TokenTextSplitter().split(docs);

// 自定义参数
TokenTextSplitter splitter = TokenTextSplitter.builder()
        .withDefaultChunkSize(800)    // 每块最大 token 数
        .withMinChunkSizeChars(350)   // 最小字符数（避免碎片化）
        .withChunkOverlap(200)        // 相邻块重叠 token 数（保持语义连贯）
        .build();
```

### 关键参数

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `chunkSize` | 500 ~ 1500 | 太小语义不完整，太大检索精度下降 |
| `chunkOverlap` | chunkSize 的 10%~25% | 防止信息在切分边界断裂 |
| `minChunkSizeChars` | 300~500 | 避免产生无意义的碎片块 |

---

## 6. 检索端（与 demo08 一致）

ETL 管道完成数据入库后，检索回答的链路与 demo08 完全一致：

```java
@GetMapping("/ask")
public String ask(@RequestParam String question) {
    // 1) 检索（数据源来自 ETL 管道解析出的文档块）
    List<Document> related = etlVectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(question)
                    .topK(3)
                    .build()
    );

    // 2) 拼资料
    String context = related.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n"));

    // 3) 增强生成
    return chatClient.prompt()
            .user(u -> u.text("""
                    请仅根据下面提供的资料回答。资料未提及的，直接说"资料中未提及"。
                    【资料】：{context}
                    【问题】：{question}
                    """)
                    .param("context", context)
                    .param("question", question))
            .call()
            .content();
}
```

---

## 7. 生产环境优化建议

### 7.1 条件化 ETL

应用启动时不需要每次都重建向量库。生产环境建议：

```java
@Bean
public SimpleVectorStore etlVectorStore(EmbeddingModel embeddingModel) {
    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

    // 检查是否存在持久化文件
    File saved = new File("vector-store.json");
    if (saved.exists()) {
        store.load(saved);   // 从文件恢复（避免重复 embedding 调用）
    } else {
        // 执行 ETL 管道...
        store.write(chunks);
        store.save(saved);   // 持久化到文件
    }
    return store;
}
```

### 7.2 增量更新

不是每次都全量重建，而是在新文档到来时增量写入：

```java
// 只处理新增的文档
List<Document> newDocs = detectNewDocuments();
if (!newDocs.isEmpty()) {
    List<Document> chunks = new TokenTextSplitter().split(newDocs);
    store.add(chunks);  // 增量添加（不是 write 覆盖）
}
```

### 7.3 持久化向量库

`SimpleVectorStore` 是内存向量库，服务重启后数据丢失。生产环境应换用：

- **PGVector**（PostgreSQL 插件）—— 适合中小团队，运维简单
- **Redis Stack** —— 低延迟，适合实时场景
- **Milvus** —— 大规模向量检索

---

## 8. ETL + RAG 完整链路图

```
┌──────────┐    ┌──────────────────┐    ┌──────────────┐
│ 原始文档  │───▶│ TikaDocumentReader│───▶│ List<Document>│
│ PDF/DOCX │    │ (Extract 抽取)    │    │              │
└──────────┘    └──────────────────┘    └──────┬───────┘
                                                │
                    ┌──────────────────┐        │
                    │ TokenTextSplitter│◀───────┘
                    │ (Transform 转换)  │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │  VectorStore     │
                    │  (Load 加载)      │
                    └────────┬─────────┘
                             │
              ┌──────────────▼──────────────┐
              │    similaritySearch(query)   │ ← 用户提问
              └──────────────┬──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │  ChatClient + context       │ ← RAG 问答
              └─────────────────────────────┘
```

---

## 下一步

了解 ETL 后，看看如何通过 Advisor 扩展点增强调用链：[07 · Advisor 扩展点](./07-advisor-guide.md)。
