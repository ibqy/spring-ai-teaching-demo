# 07 · Advisor 扩展点

> **前置 Demo**：demo06（理解 ChatMemory Advisor）  
> **涉及 Demo**：demo12  
> **目标**：掌握自定义 Advisor 的实现，覆盖日志记录、耗时统计、内容过滤等实战场景。

---

## 1. Advisor 是什么？

**Advisor（顾问）** 是 Spring AI 最强大的扩展机制，它像 AOP 的"环绕通知"，可以在每次模型调用的**前后**插入自定义逻辑：

```
请求 → [Advisor1] → [Advisor2] → ... → 大模型 → ... → [Advisor2] → [Advisor1] → 响应
        ↑ 前置逻辑                                                ↑ 后置逻辑
```

内置 Advisor：

| Advisor | 功能 |
|---------|------|
| `MessageChatMemoryAdvisor` | 会话记忆，自动注入历史上下文（demo06/11） |
| `SimpleLoggerAdvisor` | 请求/响应日志 |
| **自定义 Advisor** | 日志统计、安全审计、缓存、限流……（demo12） |

---

## 2. CallAdvisor 接口

实现一个自定义 Advisor 需要实现 `CallAdvisor` 接口（它同时继承 `Advisor` 和 `Ordered`）：

```java
public interface CallAdvisor extends Advisor, Ordered {
    ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain);
}
```

三个关键点：

| 方法 | 作用 |
|------|------|
| `getName()` | 顾问名称（日志/调试标识） |
| `getOrder()` | 执行顺序（数字越小优先级越高） |
| `adviseCall(...)` | **核心方法**：在调用前/后插入逻辑。**必须调用 `chain.nextCall(request)`**，否则模型不会被调用 |

---

## 3. 实战一：日志 + 耗时统计

demo12 实现了一个记录请求内容与调用耗时的日志顾问：

```java
@Component
public class Demo12LoggingAdvisor implements CallAdvisor {

    private static final Logger log = System.getLogger(Demo12LoggingAdvisor.class.getName());

    @Override
    public String getName() {
        return "loggingAdvisor";
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;  // 排最后执行
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // ---- 调用前：记录请求内容 ----
        log.log(INFO, "[advisor] 收到请求: {0}", request.prompt().getContents());

        long start = System.currentTimeMillis();

        // ---- 真正调用模型（必须走 chain） ----
        ChatClientResponse response = chain.nextCall(request);

        // ---- 调用后：统计耗时 ----
        long cost = System.currentTimeMillis() - start;
        log.log(INFO, "[advisor] 调用完成，耗时 {0} ms", cost);
        return response;
    }
}
```

### 注册到 ChatClient

```java
public Demo12AdvisorController(ChatClient.Builder chatClientBuilder,
                                Demo12LoggingAdvisor loggingAdvisor) {
    this.chatClient = chatClientBuilder
            .defaultAdvisors(loggingAdvisor)   // 挂到默认调用链
            .build();
}
```

每次调用大模型时，控制台会输出：

```
[advisor] 收到请求: [UserMessage{...}]
[advisor] 调用完成，耗时 1234 ms => ...
```

---

## 4. 实战二：内容安全过滤

在 Advisor 中可以对请求和响应做安全检查：

```java
@Component
public class ContentFilterAdvisor implements CallAdvisor {

    private static final List<String> BLOCKED_WORDS = List.of("恶意关键词1", "恶意关键词2");

    @Override
    public String getName() { return "contentFilterAdvisor"; }

    @Override
    public int getOrder() { return 0; }  // 优先级最高，最先执行

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 调用前：检查用户输入
        String userText = request.prompt().getContents().toString();
        for (String word : BLOCKED_WORDS) {
            if (userText.contains(word)) {
                throw new IllegalArgumentException("输入包含不当内容");
            }
        }

        // 正常调用
        ChatClientResponse response = chain.nextCall(request);

        // 调用后：检查模型输出
        String output = response.chatResponse().toString();
        for (String word : BLOCKED_WORDS) {
            if (output.contains(word)) {
                // 替换或拒绝输出
                log.log(WARNING, "模型输出包含敏感词，已拦截");
            }
        }
        return response;
    }
}
```

---

## 5. 实战三：Token 成本统计

```java
@Component
public class CostTrackingAdvisor implements CallAdvisor {

    @Override
    public String getName() { return "costTrackingAdvisor"; }

    @Override
    public int getOrder() { return 100; }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        // 从响应中提取 Token 用量（模型返回的 usage 信息）
        var usage = response.chatResponse().getMetadata().getUsage();
        if (usage != null) {
            log.log(INFO, "Token 用量: 输入 {0} + 输出 {1} = 总计 {2}",
                    usage.getPromptTokens(),
                    usage.getGenerationTokens(),
                    usage.getTotalTokens());
        }
        return response;
    }
}
```

---

## 6. 实战四：结果缓存

对于重复的常见问题，可以用 Advisor 实现一个简单的缓存层：

```java
@Component
public class CacheAdvisor implements CallAdvisor {

    private final Map<String, ChatClientResponse> cache = new ConcurrentHashMap<>();

    @Override
    public String getName() { return "cacheAdvisor"; }

    @Override
    public int getOrder() { return -100; }  // 最先执行（在调用模型前拦截）

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String cacheKey = hash(request.prompt());

        // 缓存命中：直接返回，不再调用模型
        ChatClientResponse cached = cache.get(cacheKey);
        if (cached != null) {
            log.log(INFO, "缓存命中");
            return cached;
        }

        // 缓存未命中：正常调用并缓存结果
        ChatClientResponse response = chain.nextCall(request);
        cache.put(cacheKey, response);
        return response;
    }

    private String hash(Prompt prompt) { /* 对提示词做哈希 */ }
}
```

---

## 7. Advisor 执行顺序与组合

多个 Advisor 按 `getOrder()` 返回值排序执行，数字越小越先执行：

```
Order  -100: CacheAdvisor      (缓存拦截：命中则短路)
Order     0: ContentFilterAdvisor (安全检查)
Order   100: CostTrackingAdvisor  (成本统计)
Order 65535: LoggingAdvisor       (日志记录)
```

注册方式：

```java
this.chatClient = chatClientBuilder
        .defaultAdvisors(
                cacheAdvisor,
                contentFilterAdvisor,
                costTrackingAdvisor,
                loggingAdvisor
        )
        .build();
```

也可以运行时动态追加（不影响全局默认）：

```java
chatClient.prompt()
    .user(message)
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .advisors(new CustomAdvisor())  // 仅本次调用生效
    .call()
    .content();
```

---

## 8. Advisor 设计原则

| 原则 | 说明 |
|------|------|
| **必须走 chain** | 在 `adviseCall` 中**必须**调用 `chain.nextCall(request)`，否则模型不会被调用 |
| **单一职责** | 一个 Advisor 只做一件事（日志监控的不要混入内容过滤） |
| **幂等性** | Advisor 可能被多次调用（重试场景），逻辑应保证幂等 |
| **轻量化** | 不要在 Advisor 中做重 IO 或长时间阻塞 |

---

## 9. 扩展思路

掌握了 Advisor 套路后，你可以实现：

- **限流 Advisor**：令牌桶/滑动窗口限制 QPS
- **审计 Advisor**：记录每次调用的用户、时间、内容（合规要求）
- **降级 Advisor**：模型超时时返回兜底回答
- **A/B 测试 Advisor**：随机路由到不同的模型/参数配置
- **多租户 Advisor**：根据租户 ID 注入不同的 system message / 知识库

---

## 下一步

了解 Advisor 扩展点后，看看结构化输出的进阶用法：[08 · 结构化输出](./08-structured-output-guide.md)。
