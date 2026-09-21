# 电商售后助手 · 综合实战

> demo18：综合运用 Tool + Skill + RAG + MCP 构建真实的电商售后 AI 助手

---

## 实战背景

这是一个完整的电商售后场景 AI 助手，综合运用了 Spring AI 的多个核心能力：

- **Tool（工具调用）**：查询订单、创建售后申请 —— 模型自主决定何时调用
- **Skill（技能文档）**：加载售后流程正文，让模型按标准流程回答
- **RAG（检索增强）**：从售后规则文档中检索相关内容，带来源引用
- **MCP（外部服务）**：调用物流查询、预约取件等外部 MCP 服务
- **意图识别 + 流程编排**：后端代码控制多步骤执行流程

## 架构图

```
  React 聊天页（提问/确认/SSE 展示）
        ↓
  Spring Boot + Spring AI 2.0（后端组织执行）
   ├─ 本地能力：Tool（订单与申请）+ Skill（流程正文）
   ─ RAG：检索售后规则，返回资料来源
        ↓
  ├─ 本地模型：生成参数和回答、计算知识向量
  └─ 独立 MCP 服务：查物流、预约取件
        ↓
  PostgreSQL + pgvector（订单/会话/事件/知识向量）
```

## 核心代码

### 1. Controller 构造

```java
@RestController
@RequestMapping("/api/demo18")
public class Demo18EcommerceAfterSalesController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public Demo18EcommerceAfterSalesController(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) VectorStore vectorStore) {
        ChatClient.Builder builder = chatClientBuilder;
        if (vectorStore != null) {
            builder = builder.defaultAdvisors(
                    new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults())
            );
        }
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }
}
```

### 2. 智能问答（RAG）

```java
@GetMapping("/chat")
public Map<String, Object> chat(@RequestParam String question) {
    String answer = chatClient.prompt()
            .user(question)
            .call()
            .content();
    return Map.of(
            "question", question,
            "answer", answer,
            "source", "AI 助手（Tool + Skill + RAG）"
    );
}
```

### 3. 工具调用（订单查询）

```java
@GetMapping("/order/{orderId}")
public Map<String, Object> getOrder(@PathVariable String orderId) {
    Map<String, Object> order = ORDER_DB.get(orderId);
    if (order == null) {
        return Map.of("error", "订单不存在");
    }
    return order;
}
```

### 4. 技能文档（Skill）

```java
@GetMapping("/skill/after-sales")
public Map<String, Object> getAfterSalesSkill() {
    return Map.of(
            "skillName", "电商售后流程",
            "version", "1.0",
            "steps", List.of(
                    Map.of("step", 1, "name", "确认订单信息"),
                    Map.of("step", 2, "name", "了解问题原因"),
                    Map.of("step", 3, "name", "说明售后政策"),
                    Map.of("step", 4, "name", "引导申请流程"),
                    Map.of("step", 5, "name", "跟进处理进度")
            )
    );
}
```

### 5. MCP 服务（物流查询）

```java
@GetMapping("/logistics")
public Map<String, Object> queryLogistics(@RequestParam String orderId) {
    // 模拟调用外部 MCP 物流服务
    return Map.of(
            "orderId", orderId,
            "logistics", "顺丰速运 SF1234567890",
            "tracking", tracking,
            "source", "MCP 物流服务（模拟）"
    );
}
```

## 接口列表

| 接口 | 方法 | 说明 |
|-----|------|------|
| `/api/demo18/chat?question=...` | GET | 智能问答（带 RAG） |
| `/api/demo18/order/{orderId}` | GET | 查询订单详情 |
| `/api/demo18/apply` | POST | 创建售后申请 |
| `/api/demo18/logistics?orderId=...` | GET | 查询物流（MCP） |
| `/api/demo18/pickup` | POST | 预约取件（MCP） |
| `/api/demo18/skill/after-sales` | GET | 获取技能文档 |
| `/api/demo18/rag/search?query=...` | GET | RAG 检索规则 |
| `/api/demo18/demo` | GET | 完整流程演示 |

## 完整流程演示

访问 `/api/demo18/demo` 查看一次完整的售后对话流程：

1. **用户提问**："我买的 iPhone 屏幕碎了，怎么申请售后？"
2. **AI 识别意图**：调用 Tool 查询订单
3. **用户提供订单号**：ORD-20260921-001
4. **AI 查询订单 + RAG 检索政策**：确认在 7 天无理由退货期内
5. **引导申请**：询问用户希望维修还是换货
6. **用户选择**：我要换货
7. **创建申请 + 预约取件**：Tool + MCP 协同完成

## 资源文件

### 技能文档

`src/main/resources/skills/after-sales/SKILL.md`：

```yaml
---
name: after-sales
description: 电商售后流程技能
---

# 电商售后处理流程

## 处理步骤
1. 确认订单信息
2. 了解问题原因
3. 说明售后政策
4. 引导申请流程
5. 跟进处理进度
```

### RAG 文档

`src/main/resources/docs/after-sales-rules.md`：

包含完整的售后规则：
- 7 天无理由退货政策
- 15 天质量问题换货
- 1 年保修维修
- 特殊商品政策
- 运费规则

## 教学要点

### 1. 多能力协同

demo18 展示了如何让 AI 助手协同使用多种能力：
- **Tool**：执行具体操作（查询、创建）
- **Skill**：提供流程指导（按标准流程回答）
- **RAG**：提供知识支撑（检索规则文档）
- **MCP**：调用外部服务（物流、取件）

### 2. 流程编排

后端代码控制多步骤执行流程，而不是完全交给模型自由发挥：
- 先确认订单，再说明政策
- 先了解问题，再引导申请
- 先创建申请，再预约取件

### 3. 真实场景

这是一个可以直接用于生产的架构：
- 订单数据来自数据库
- 规则文档来自知识库
- 物流服务来自外部 MCP
- AI 负责理解意图、组织流程

## 下一步

- 接入真实的 PostgreSQL + pgvector 持久化数据
- 将 MCP 服务独立部署（物流、取件）
- 添加 SSE 流式输出，提升用户体验
- 集成前端 React 聊天页面

---

**相关 Demo**：
- [demo08 · RAG 检索增强](/03-rag-deep-dive)
- [demo10 · MCP 协议](/10-mcp-guide)
- [demo11 · Agent Skills](/05-agent-skills-guide)
- [demo04 · 工具调用](/04-tool-calling-guide)
