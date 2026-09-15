# 09 · 生产部署清单

> **前置 Demo**：全部 demo01 ~ demo14  
> **涉及 Demo**：demo11（组合实战）、demo12（Advisor）、demo13（ETL）、demo14（Skills）  
> **目标**：将教学 Demo 推向生产环境的实战清单——安全、可观测、高可用、多租户。

---

## 1. 安全配置

### 1.1 API Key 管理

**绝对不要把 API Key 写死在代码/配置文件里。**

✅ 正确做法（环境变量）：

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}   # 无默认值，启动时缺失直接报错
```

生产环境推荐：

| 方案 | 适用场景 |
|------|----------|
| K8s Secret | Kubernetes 部署 |
| Vault / AWS Secrets Manager | 密钥统一管理 |
| 配置中心（Nacos / Apollo） | 微服务架构 |
| CI/CD 变量注入 | 自动部署流水线 |

### 1.2 输入安全

用户输入必须经过校验和过滤：

```java
// 在 Advisor 中拦截敏感词（参考 demo12）
@Component
public class InputSanitizerAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1) 长度限制
        String text = extractUserMessage(request);
        if (text.length() > 4000) {
            throw new IllegalArgumentException("输入过长");
        }

        // 2) 注入攻击防御
        if (containsPromptInjection(text)) {
            throw new IllegalArgumentException("检测到提示词注入");
        }

        return chain.nextCall(request);
    }
}
```

### 1.3 ShellTools 安全边界

如果使用 Agent Skills（demo14），必须严格限定 Shell 执行范围：

```java
ShellTools.builder()
    .workingDirectory("/app/safe-scripts")    // 限定目录
    .allowedCommands(List.of("bash", "python")) // 命令白名单
    .timeout(Duration.ofSeconds(30))            // 超时熔断
    .build();
```

### 1.4 输出安全

模型的输出内容同样需要过滤（防止 XSS、敏感信息泄露等），可在 Advisor 后置逻辑中处理。

---

## 2. 多租户考虑

如果你的应用服务多个客户/团队：

### 2.1 知识库隔离

```java
// 按租户 ID 注入不同的 VectorStore
@Configuration
public class MultiTenantConfig {

    @Bean
    public Map<String, VectorStore> tenantVectorStores(EmbeddingModel embeddingModel) {
        Map<String, VectorStore> stores = new HashMap<>();
        for (String tenantId : getTenantIds()) {
            SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
            // 加载该租户的知识库文档
            loadTenantDocs(store, tenantId);
            stores.put(tenantId, store);
        }
        return stores;
    }
}
```

### 2.2 会话记忆隔离

`MessageChatMemoryAdvisor` 已通过 `CONVERSATION_ID` 天然支持隔离：

```java
advisors(a -> a.param(ChatMemory.CONVERSATION_ID, tenantId + ":" + userId))
```

### 2.3 模型配额

为不同租户分配不同的模型调用额度，在 Advisor 中实现限流或计数。

---

## 3. 可观测性

### 3.1 核心指标

| 指标 | 说明 | 采集方式 |
|------|------|----------|
| 调用次数 | QPS / 日均调用量 | Advisor 计数 + Prometheus |
| 调用耗时 | P50 / P95 / P99 | Advisor 计时 + Micrometer |
| Token 用量 | 输入 Token / 输出 Token | Advisor 解析 usage |
| 错误率 | 模型异常 / 超时 / 限流 | 异常计数器 |
| 缓存命中率 | Cache Advisor 命中比例 | 自定义指标 |

### 3.2 日志规范

```java
// demo12 的日志模式可扩展为结构化日志
log.log(INFO, "[advisor] model={0} duration={1}ms tokens_in={2} tokens_out={3}",
        modelName, cost, promptTokens, generationTokens);
```

建议接入日志聚合平台（ELK / Loki），方便排查问题和成本分析。

### 3.3 健康检查

```java
@Component
public class ModelHealthIndicator implements HealthIndicator {

    private final ChatClient chatClient;

    @Override
    public Health health() {
        try {
            String response = chatClient.prompt().user("ping").call().content();
            return Health.up().withDetail("model", "ok").build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
```

---

## 4. 限流与熔断

### 4.1 限流策略

在 Advisor 中实现令牌桶/滑动窗口限流：

```java
@Component
public class RateLimitAdvisor implements CallAdvisor {

    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 QPS

    @Override
    public int getOrder() { return -200; }  // 最先执行

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        if (!rateLimiter.tryAcquire(Duration.ofSeconds(1))) {
            throw new RuntimeException("请求过于频繁，请稍后重试");
        }
        return chain.nextCall(request);
    }
}
```

### 4.2 熔断降级

当模型服务不可用时，返回兜底回答而非直接报错：

```java
@Override
public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    try {
        return chain.nextCall(request);
    } catch (Exception e) {
        log.log(WARNING, "模型调用失败，返回降级回答: {0}", e.getMessage());
        // 返回预定义的兜底响应
        return ChatClientResponse.builder()
                .chatResponse(new ChatResponse(List.of(
                        new Generation("抱歉，AI 服务暂时不可用，请稍后重试。")))
                ).build();
    }
}
```

---

## 5. 缓存策略

### 5.1 什么该缓存

| 可缓存 | 不可缓存 |
|--------|----------|
| 知识库文档内容（RAG 资料） | 有时效性的回答（天气、股价） |
| 高频热问的生成结果 | 个性化回答（含用户上下文） |
| Embedding 向量（文档未变则向量不变） | 工具调用结果（订单状态实时变化） |

### 5.2 多级缓存方案

```
请求 → L1: 本地缓存(Caffeine) → L2: 分布式缓存(Redis) → L3: 重新调用模型
```

- **L1（Caffeine）**：纳秒级，适合热点问题
- **L2（Redis）**：毫秒级，跨实例共享，适合 RAG 检索结果
- **L3（重新生成）**：兜底

### 5.3 Embedding 缓存

如果文档不变，不需要每次启动都重新计算向量。`SimpleVectorStore` 支持 `save()` / `load()` 持久化：

```java
File saved = new File("/data/vector-store.json");
if (saved.exists()) {
    store.load(saved);   // 从磁盘恢复，避免重复调用 Embedding API
} else {
    // 执行 ETL 管道...
    store.save(saved);   // 持久化到磁盘
}
```

---

## 6. 错误处理与重试

### 6.1 重试机制

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder) {
    return builder
            .defaultAdvisors(new RetryAdvisor(
                    3,                          // 最大重试次数
                    Duration.ofSeconds(1),      // 初始间隔
                    Duration.ofSeconds(10),     // 最大间隔
                    2.0                          // 退避倍数
            ))
            .build();
}
```

### 6.2 常见异常处理

| 异常类型 | 原因 | 建议处理 |
|----------|------|----------|
| 401 Unauthorized | API Key 无效 | 告警 + 检查密钥配置 |
| 429 Rate Limit | 调用频率超限 | 退避重试 + 降低并发 |
| 503 Service Unavailable | 模型服务宕机 | 熔断降级 + 切换备用模型 |
| SocketTimeout | 网络超时 | 重试 + 调整 `timeout` 配置 |
| JsonMappingException | 结构化输出解析失败 | 重试 + 优化提示词约束 |

---

## 7. 部署架构参考

```
                    ┌──────────────┐
                    │   Nginx/K8s  │
                    │   负载均衡     │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ App 实例1 │ │ App 实例2 │ │ App 实例3 │
        │ ChatClient│ │ ChatClient│ │ ChatClient│
        └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
              │             │             │
    ┌─────────┼─────────────┼─────────────┼─────────┐
    │         ▼             ▼             ▼         │
    │  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
    │  │ Redis    │  │PGVector  │  │ 大模型API │    │
    │  │ 缓存+会话 │  │ 向量库    │  │ (OpenAI等)│    │
    │  └──────────┘  └──────────┘  └──────────┘    │
    └──────────────────────────────────────────────┘
```

---

## 8. 检查清单

上线前逐项确认：

- [ ] API Key 通过环境变量/密钥管理服务注入，未硬编码
- [ ] 开启了输入校验（长度限制、注入检测）
- [ ] ShellTools 限定目录和命令白名单（如使用）
- [ ] 实施了限流（令牌桶/滑动窗口）
- [ ] 配置了熔断降级（模型不可用时的兜底）
- [ ] 关键指标已接入监控（QPS、耗时、Token、错误率）
- [ ] 实现了 Embedding 缓存（避免重启重复计算）
- [ ] VectorStore 已替换为持久化方案（PGVector / Redis）
- [ ] 多租户场景已隔离（知识库、会话、配额）
- [ ] 日志采用结构化格式，接入聚合平台
- [ ] 配置了健康检查端点
- [ ] `.gitignore` 已排除密钥文件

---

## 下一步

恭喜完成全部教学文档！回顾路线：

```
01 快速开始 → 02 核心概念 → 03 RAG 深入 → 04 函数调用
    → 05 Agent Skills → 06 ETL 管道 → 07 Advisor → 08 结构化输出
    → 09 生产部署清单
```

建议打开 `docs/spring-ai-teaching-guide/spring-ai-teaching-guide.html` 交互式教学文档，结合 Demo 代码边看边跑，巩固学习效果。实际项目中 demo11 是最佳起点——它把记忆、角色、知识库、工具组合成了一个完整的智能客服。从它开始裁剪，接入你自己的业务数据和工具，就能快速上线第一个 AI 应用。
