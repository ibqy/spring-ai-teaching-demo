# 03 · RAG 深入

> **前置 Demo**：demo01 ~ demo07（掌握 ChatClient 基本操作）  
> **涉及 Demo**：demo08、demo13  
> **目标**：理解 RAG 的完整链路——文档加载→切分→向量化→检索→增强生成。

---

## 1. 什么是 RAG？

**RAG（Retrieval-Augmented Generation，检索增强生成）** 是大模型 + 私有知识库的经典方案。

大模型训练数据有截止日期，它不可能知道"你们公司上个月的运营数据"或"店铺的营业时间"。RAG 的核心思路是：

> 在提问前，先到你的知识库里**检索**相关文档，把文档内容作为"参考资料"**拼进提示词**，再让模型**基于资料回答**。

```
用户提问 "营业时间是几点？"
        │
        ▼
┌───────────────────┐
│ 1. 检索 (Retrieve) │  ← 到向量库找最相关的文档片段
└───────┬───────────┘
        ▼
┌───────────────────┐
│ 2. 增强 (Augment)  │  ← 把资料拼进提示词
└───────┬───────────┘
        ▼
┌───────────────────┐
│ 3. 生成 (Generate) │  ← 模型基于资料回答
└───────┬───────────┘
        ▼
    "每天 9:00 到 21:00，周末照常营业"
```

---

## 2. 核心组件

| 组件 | 职责 |
|------|------|
| `Document` | 文本块载体，含文本内容和元数据 |
| `DocumentReader` | 从文件系统/URL 读取原始文档 |
| `DocumentTransformer` | 文本切分/清洗（如 TokenTextSplitter） |
| `EmbeddingModel` | 把文本转成向量 |
| `VectorStore` | 存储向量 + 相似度检索 |
| `SearchRequest` | 检索请求（query + topK + 相似度阈值） |

---

## 3. demo08：手写版 RAG

demo08 用最直白的方式演示 RAG 的核心流程：

### 3.1 构建向量库（RagConfig）

```java
@Bean
public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

    // 读取知识库文件
    String raw = new ClassPathResource("kb/shop-intro.txt")
            .getContentAsString(StandardCharsets.UTF_8);

    // 手工按段落切分
    List<Document> docs = new ArrayList<>();
    for (String para : raw.split("\\n\\n")) {
        if (!para.isBlank()) {
            docs.add(new Document(para.trim()));
        }
    }

    // 向量化并写入
    store.add(docs);
    return store;
}
```

### 3.2 检索 + 增强生成（控制器）

```java
@GetMapping("/ask")
public String ask(@RequestParam String question) {
    // 1) 检索：把问题转成向量，到向量库找 topK 最相似的片段
    List<Document> related = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query(question)
                    .topK(3)
                    .build()
    );

    // 2) 增强：把检索结果拼成"参考资料"文本
    String context = related.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n"));

    // 3) 生成：资料 + 问题一起交给模型
    return chatClient.prompt()
            .user(u -> u.text("""
                    请仅根据下面提供的资料回答用户问题。
                    如果资料里没有相关内容，请直接回答"资料中未提及"。

                    【资料】：{context}
                    【问题】：{question}
                    """)
                    .param("context", context)
                    .param("question", question))
            .call()
            .content();
}
```

### 3.3 关键细节

- **`SearchRequest.topK(3)`**：返回与问题最相似的 3 个片段。topK 越大上下文越丰富，但也越容易引入噪音。
- **"请仅根据资料回答"**：这条指令非常重要，防止模型在找不到答案时编造（幻觉）。
- **手工切分**：`split("\\n\\n")` 按段落切分，简单但不专业——对于跨段落语义、长文档效果有限。

---

## 4. demo13：标准化 ETL 管道

demo13 对应的是"知识库里的数据从哪来"。相比 demo08 的手工切分，demo13 使用了标准 ETL 管道（详见 [06 · ETL 文档管道](./06-etl-pipeline-guide.md)）。

核心差异对比：

| 维度 | demo08 | demo13 |
|------|--------|--------|
| 文档格式 | 仅 txt | PDF/Word/HTML/Markdown 等（Tika 万能读取） |
| 切分方式 | 手工 `split("\n\n")` | `TokenTextSplitter` 按 token 智能切分 |
| 读取方式 | 硬编码读取单文件 | `PathMatchingResourcePatternResolver` 扫描目录 |
| 适用场景 | 简单教学演示 | 生产级文档接入 |

---

## 5. 检索策略调优

### 5.1 topK 选择

- 太小的 topK（如 1）：可能错过关键信息
- 太大的 topK（如 10）：噪音多、token 消耗大、回答可能跑偏
- 推荐：**3~5** 是经验平衡点

### 5.2 相似度阈值

```java
SearchRequest.builder()
    .query(question)
    .topK(5)
    .similarityThreshold(0.7)   // 只返回相似度 ≥ 0.7 的结果
    .build();
```

设置相似度阈值可以过滤掉"不相关的匹配"，避免模型读到无关资料。

### 5.3 提示词模板的写法

```
❌ 糟糕：直接拼接资料，模型可能忽略
"资料：{context}，问题：{question}"

✅ 推荐：明确约束，结构化分隔
"""
请仅根据下面提供的资料回答用户问题。
如果资料里没有相关内容，请直接回答"资料中未提及"。

【资料】：
{context}

【问题】：{question}
"""
```

---

## 6. RAG 与函数调用的对比

两个技术都让模型能访问"外部数据"，但机制不同：

| 维度 | RAG | 函数调用 |
|------|-----|----------|
| 谁发起 | **应用**提前检索，喂给模型 | **模型**判断需要时，主动请求调用工具 |
| 数据性质 | 静态知识（文档、政策） | 动态数据（订单状态、实时天气） |
| 数据存储 | 向量库 | API / 数据库 / Java 方法 |
| 典型场景 | 客服知识库、公司章程 | 查订单、查天气、查库存 |

> 实际项目中两者经常**组合使用**（如 demo11）：知识库回答政策类问题，函数调用查询实时数据。

---

## 7. 多向量库管理

当应用需要多个知识库时（如 demo08 和 demo13 各有一个向量库），用 `@Qualifier` 按名称注入：

```java
// demo08 的向量库
@Bean
public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) { ... }

// demo13 的向量库
@Bean
public SimpleVectorStore etlVectorStore(EmbeddingModel embeddingModel) { ... }

// 注入时指名道姓
public Demo13EtlController(
        @Qualifier("etlVectorStore") VectorStore etlVectorStore) { ... }
```

这是生产项目的常见模式：按业务域拆分多个向量库，各自独立注入。

---

## 下一步

了解了 RAG 的检索端和 ETL 端之后，深入函数调用机制：[04 · 函数调用指南](./04-tool-calling-guide.md)。
