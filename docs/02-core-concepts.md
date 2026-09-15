# 02 · 核心概念

> **前置 Demo**：demo01、demo02  
> **涉及 Demo**：demo01 ~ demo08（贯穿全部核心接口）  
> **目标**：理解 Spring AI 的核心接口体系与职责分工。

---

## 1. 核心接口概览

Spring AI 2.0 将 AI 应用开发抽象为一组清晰的接口，各司其职：

```
┌──────────────────────────────────────────────┐
│              ChatClient（门面）                │
│    .prompt() → .call()/.stream() → .content()│
├──────────────────────────────────────────────┤
│   ChatModel    │  VectorStore   │  Tool       │
│   (对话模型)    │  (向量存储)    │  (函数调用)   │
├──────────────────────────────────────────────┤
│  EmbeddingModel │  Advisor      │  Prompt     │
│  (嵌入模型)      │  (调用顾问)    │  (提示词)    │
└──────────────────────────────────────────────┘
```

这些接口的关系：**ChatClient 是面向开发者的统一入口，它将底层组件（ChatModel、Tool、Advisor、VectorStore）编排在一起**，屏蔽复杂性。

---

## 2. ChatClient —— 核心门面

`ChatClient` 是开发者日常打交道最多的对象。它提供流式（Fluent）API，一条链式调用完成一次对话：

```java
// 基础用法（demo01）
chatClient.prompt()
    .user("你好")            // 用户消息
    .call()                  // 同步调用
    .content();              // 取出文本

// 流式用法（demo02）
chatClient.prompt()
    .user("写一首短诗")
    .stream()                // 流式调用
    .content();              // Flux<String>
```

`ChatClient.Builder` 是原型 Bean（`@Scope("prototype")`），Spring AI 自动配置并注入所需的默认组件。构建时可注册全局 Advisor、System Message、ToolCallbacks：

```java
// 带全局配置的构建（demo11 示例）
ChatClient chatClient = chatClientBuilder
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())  // 全局记忆
    .defaultTools(afterSalesTool)           // 全局工具
    .defaultSystem("你是客服小北...")        // 全局人设
    .build();
```

---

## 3. ChatModel —— 对话模型

`ChatModel` 是对大模型 API 的底层封装。通常情况下你不需要直接用它，而是通过 `ChatClient` 间接调用。Spring AI 的 `spring-ai-starter-model-openai` 自动配置了一个 OpenAI 兼容的 `ChatModel` 实现。

配置方式（`application.yml`）：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:demo}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
          temperature: 0.7
```

---

## 4. Prompt —— 提示词

Prompt 是一次对话的完整描述。Spring AI 支持模板化提示词，用 `{变量}` 声明占位符：

```java
// demo04：提示词模板
chatClient.prompt()
    .user(u -> u
        .text("请你写一首关于 {topic} 的五言绝句，要求押韵、有意境。")
        .param("topic", topic))
    .call()
    .content();
```

一次 Prompt 可包含多种角色的消息：

| 消息类型 | 对应方法 | 用途 |
|----------|----------|------|
| SystemMessage | `.system(...)` | 设定模型角色、语气、规则（demo07） |
| UserMessage | `.user(...)` | 用户本轮输入（每次请求必含） |
| AssistantMessage | 模型自动生成 | 模型的回答（在记忆回放时出现） |

系统消息示例（demo07）：

```java
chatClient.prompt()
    .system(s -> s
        .text("你是一位资深的 {role}，回答问题时请专业、简洁。")
        .param("role", "Java高级工程师"))
    .user(question)
    .call()
    .content();
```

---

## 5. Tool / Function Calling —— 函数调用

`@Tool` 注解将一个 Java 方法标记为"模型可调用的工具"。模型在推理时发现需要动态数据时，会自动请求调用这些工具，拿到结果后再组织自然语言回答。

```java
// demo05：天气查询工具
@Component
public class WeatherTools {

    @Tool(description = "查询指定城市的当前天气，用于回答天气相关问题")
    public String getWeather(String city) {
        return WEATHER.getOrDefault(city, "暂无该城市天气数据。");
    }
}
```

使用时只需 `.tools(...)`：

```java
chatClient.prompt()
    .user("杭州今天天气怎么样？")
    .tools(new WeatherTools())
    .call()
    .content();
```

调用时序：用户提问 → 模型判断需要工具 → Spring AI 自动调用 `getWeather("杭州")` → 拿到 "晴，26℃" → 模型组织自然语言回答。

---

## 6. VectorStore —— 向量存储

`VectorStore` 是 RAG（检索增强生成）的核心基础设施。它把文本转成向量后存储，支持相似度检索。

本项目使用 `SimpleVectorStore`（内存向量库，教学用）：

```java
// demo08：构建向量库
SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

// 写入文档
Document doc = new Document("欢迎光临小北优选咖啡店...");
store.add(List.of(doc));

// 检索
List<Document> results = store.similaritySearch(
    SearchRequest.builder().query("营业时间").topK(3).build()
);
```

生产环境可替换为 PGVector、Redis、Milvus 等持久化向量库，API 不变。

---

## 7. EmbeddingModel —— 嵌入模型

`EmbeddingModel` 负责将文本转换为向量（浮点数组），是 `VectorStore` 实现相似度检索的前提。

配置：

```yaml
spring:
  ai:
    openai:
      embedding:
        options:
          model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
```

通常情况下你不需要直接调用 `EmbeddingModel`——`VectorStore` 内部会自动使用它。

---

## 8. Advisor —— 调用顾问

Advisor 是 Spring AI 最强大的扩展机制。它像 AOP 切面，在每次模型调用的前后插入自定义逻辑。

```java
// demo12：日志顾问
@Component
public class Demo12LoggingAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        log.log(INFO, "[advisor] 收到请求: {0}", request.prompt().getContents());
        long start = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);  // 必须调用 chain
        log.log(INFO, "[advisor] 耗时 {0} ms", System.currentTimeMillis() - start);
        return response;
    }
}
```

内置 Advisor：

| Advisor | 用途 |
|---------|------|
| `MessageChatMemoryAdvisor` | 会话记忆（demo06/11） |
| `SimpleLoggerAdvisor` | 请求/响应日志 |
| 自定义 Advisor | 敏感词过滤、缓存、限流等（demo12） |

---

## 9. 结构化输出

Spring AI 支持将模型的 JSON 回答**直接反序列化为 Java 对象**：

```java
// 单个对象（demo03）
Demo03ActorFilm result = chatClient.prompt()
    .user("请为演员 周星驰 自动生成他的 2 部代表作。")
    .call()
    .entity(Demo03ActorFilm.class);  // 自动 JSON → Java

// 对象列表（demo10）
List<Demo10Order> list = chatClient.prompt()
    .user("请从中抽取所有订单...")
    .call()
    .entity(new ParameterizedTypeReference<List<Demo10Order>>() {});
```

底层逻辑：Spring AI 提示模型以 JSON 格式输出，然后自动反序列化。

---

## 10. 概念关系总结

```
用户请求
   │
   ▼
ChatClient.prompt()         ← 统一门面
   │
   ├── .system(...)         ← 系统消息（人设）
   ├── .user(...)           ← 用户消息
   ├── .advisors(...)       ← Advisor 切面链
   │     ├── MemoryAdvisor  ← 自动注入上下文历史
   │     └── CustomAdvisor  ← 日志/统计/过滤
   ├── .tools(...)          ← 模型可调用的工具
   └── .call() / .stream()
         │
         ▼
      ChatModel             ← 真正调用大模型
         │
         ▼
      ChatClientResponse    ← 封装模型回答
         │
         ├── .content()     ← 纯文本
         └── .entity(...)   ← 结构化对象
```

---

## 下一步

理解核心概念后，深入 RAG 实战：[03 · RAG 深入](./03-rag-deep-dive.md)。
