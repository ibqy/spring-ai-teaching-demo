# 15 · Token 用量可观测实战

> **前置 Demo**：demo12（自定义 Advisor 扩展点）
> **涉及 Demo**：demo21
> **目标**：掌握用 Advisor 做横切关注点收口，给 AI 应用装上「电表」——Token 用量统计与成本估算。

---

## 1. 为什么用量可观测是生产第一课？

大模型按 Token 计费。没有用量监控，就等于：

- 不知道钱花在哪（哪个功能、哪个用户、哪个模型）
- 无法发现异常消耗（死循环调工具、提示词膨胀、被恶意刷量）
- 容量与成本规划全靠猜

Spring AI 的每个响应都带 `Usage` 元数据（输入/输出 Token 数），demo21 演示如何用 **Advisor** 这个扩展点把它累计起来。

---

## 2. Advisor：横切关注点的收口机制

回顾 demo12：Advisor 像 AOP 切面，能在「调用大模型」前后插入逻辑。logging  Advisor 只是打印；把同样的机制用于**聚合统计**，就是计量器：

```
请求 ──► [Advisor 链] ──► ChatModel ──► 响应
              │
     Demo21TokenAccountingAdvisor
     ┌────────────────────────────┐
     │ before: 记录开始（可限流/鉴权）│
     │ after:  从 Usage 累计 Token  │
     └────────────────────────────┘
```

**为什么用 Advisor 而不是在控制器里统计？**

| 维度 | Advisor | 控制器里统计 |
|---|---|---|
| 代码侵入 | 零侵入（挂一次，全局生效） | 每个调用点都要写 |
| 遗漏风险 | 无（所有调用必经 advisor 链） | 高（总有忘记调的接口） |
| 逻辑复用 | 一个 Bean，全应用共享 | 复制粘贴 |
| 可测试性 | 核心逻辑可独立单测 | 耦合 Web 层 |

这正是 demo12 教学要点的延伸：**横切逻辑用横切机制解决**。

---

## 3. demo21 实现要点

### 3.1 记账核心（从响应拿 Usage）

```java
@Override
public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
    ChatClientResponse response = chain.nextCall(request);   // 先放行，让链路继续走到模型
    if (response != null && response.chatResponse() != null) {
        record(response.chatResponse());                     // 调用后记账，只读不改
    }
    return response;
}

void record(ChatResponse chatResponse) {
    callCount.increment();
    Usage usage = chatResponse.getMetadata() == null ? null : chatResponse.getMetadata().getUsage();
    if (usage == null) return;                               // 容错：部分模型不返回用量
    if (usage.getPromptTokens() != null) promptTokens.add(usage.getPromptTokens());
    if (usage.getCompletionTokens() != null) completionTokens.add(usage.getCompletionTokens());
}
```

### 3.2 并发安全：为什么用 LongAdder

Advisor 是**单例 Bean**，并发请求会同时写计数器：

- `synchronized`：简单但高并发下争用严重
- `AtomicInteger`：CAS 自旋，多写场景仍有竞争
- **`LongAdder`**：内部分段（Cell 数组），多线程写不同段，最后 `sum()` 汇总——「多写少读」计数的标准选择

### 3.3 成本估算

单价（元/百万 Token）通过配置注入，Advisor 只做算术：

```yaml
demo21:
  prompt-price-per-million: 1.1      # 输入单价，按所用模型真实价格填
  completion-price-per-million: 4.4  # 输出单价
```

```java
double cost = prompt / 1_000_000.0 * promptPricePerMillion
            + completion / 1_000_000.0 * completionPricePerMillion;
```

### 3.4 健壮性：三种「没有用量」的情况都要容错

| 情况 | 处理 |
|---|---|
| `getMetadata()` 为 null | 判空跳过，调用计数仍然 +1 |
| `getUsage()` 为 null（部分模型/代理不返回） | 判空跳过 |
| Usage 字段为 null（如 `new DefaultUsage(null, null)`） | 逐字段判空累加 |

> 真实踩坑记录：Spring AI 2.x 的 `ChatResponseMetadata.builder()` 即使不调 `.usage()` 也会填充一个**空 Usage**（字段为 null）；单参数构造的 `ChatResponse` 同样如此。计量器必须把这些情况当 0 Token 处理而不是抛 NPE——`Demo21TokenAccountingAdvisorTest` 把这些分支全部固定住了。

---

## 4. 使用方式

```bash
# 1) 产生几次调用（每次都会被自动计量）
curl "http://localhost:8080/api/demo21/chat?message=用一句话介绍Spring AI"

# 2) 查看累计用量
curl http://localhost:8080/api/demo21/usage
```

```json
{
  "callCount": 1,
  "meteredCallCount": 1,
  "promptTokens": 9,
  "completionTokens": 16,
  "totalTokens": 25,
  "avgTokensPerMeteredCall": 25,
  "estimatedCostYuan": 0.0,
  "priceNote": "未配置模型单价（application.yml 的 demo21.*.price-per-million），成本按 0 展示"
}
```

```bash
# 3) 清零重新计量
curl http://localhost:8080/api/demo21/usage/reset
```

---

## 5. 从 Demo 到生产：还差什么？

demo21 是进程内计数器（重启即清零）。生产环境按这个路线演进：

1. **外部化存储**：用量写入 Redis / InfluxDB / Prometheus，而不是内存 `LongAdder`
2. **维度拆分**：按 `conversationId` / `userId` / `model` 分桶统计——Advisor 里能从 `ChatClientRequest` 拿到上下文，按需扩展
3. **实时告警**：单用户 Token 速率异常（如 1 分钟超过均值 10 倍）触发限流或告警
4. **预算熔断**：给租户/会话设 Token 预算，超限拒绝请求（参考 agent-orchestration-demo 的 Token 预算防护）
5. **与链路追踪结合**：把用量写进 OTel Span 属性，一次请求的「成本画像」完整可查（agent-orchestration-demo 的 observe 模块有示范）
6. **计费对账**：聚合数据与模型厂商账单按日核对，及时发现计量偏差

---

## 6. 小结

- Advisor 是 Spring AI 的横切扩展点：日志、计量、限流、审计都用同一个机制
- 计量三要素：**准确**（从响应 Usage 拿数）、**安全**（LongAdder + 全分支容错）、**可查**（快照端点）
- 生产化路径：外部存储 → 维度拆分 → 告警 → 预算熔断 → 链路追踪
