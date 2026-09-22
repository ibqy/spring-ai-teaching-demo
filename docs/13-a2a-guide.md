# 13 · A2A 智能体间通信

> 前置知识：demo16（MCP） | 涉及 Demo：demo19 | 学习目标：理解 A2A 协议，让不同 Agent 互相发现和调用

## 1. 什么是 A2A？

> **A2A（Agent-to-Agent Protocol）** 是 Google 2025 年提出的智能体间通信标准。

如果把 MCP 比作"AI 的 USB-C 接口"（连接工具），那 A2A 就是"AI 的外交通道"——让不同框架构建的 Agent 能互相发现、互相协作。

```
┌─────────────┐    A2A 协议     ┌─────────────┐
│  Spring AI  │ ←────────────→ │  LangChain   │
│   Agent A   │   JSON-RPC     │   Agent B    │
└─────────────┘                └─────────────┘
       ↕ MCP                          ↕ MCP
  ┌─────────┐                    ┌─────────┐
  │ 工具服务 │                    │ 工具服务 │
  └─────────┘                    └─────────┘
```

### MCP vs A2A 对比

| 维度 | MCP | A2A |
|------|-----|-----|
| 解决什么 | AI ↔ 工具 | Agent ↔ Agent |
| 类比 | USB-C 接口 | 外交通道 |
| 核心概念 | Tool / Server / Client | AgentCard / Task / JSON-RPC |
| 提出者 | Anthropic (2024) | Google (2025) |
| 采纳者 | OpenAI / Google / 微软 | Salesforce / Atlassian / 微软等 |

## 2. A2A 的三个核心概念

| 概念 | 作用 | 本项目对应 |
|------|------|-----------|
| **AgentCard** | Agent 的"名片"：名称、描述、技能、端点 | `spring-ai-a2a-server` 自动暴露 `/a2a/card` |
| **AgentExecutor** | 接收远程请求，交给本地 ChatClient 处理 | `DefaultAgentExecutor` 包装 ChatClient |
| **JSON-RPC 2.0** | 通信格式：标准化的请求/响应 | `POST /a2a/` 端点 |

## 3. demo19 在项目中的实现

### 3.1 依赖

```xml
<!-- A2A 服务端自动配置（社区项目） -->
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-server-autoconfigure</artifactId>
    <version>0.3.0</version>
</dependency>
```

### 3.2 配置

```yaml
spring:
  ai:
    a2a:
      server:
        enabled: ${A2A_ENABLED:false}  # 默认关闭
```

### 3.3 核心代码

```java
@RestController
@RequestMapping("/api/demo19")
public class Demo19A2aController {
    // 1) 注入 ChatClient，构建本地对话能力
    // 2) 读取 A2A 启用状态
    // 3) /ask 端点：本地测试对话
    // 4) /status 端点：查看 AgentCard 信息
}
```

启用 A2A 后，框架自动暴露：
- `GET /a2a/card` —— 标准 AgentCard（其他 Agent 通过此发现你的能力）
- `POST /a2a/` —— JSON-RPC 消息处理端点

### 3.4 运行

```bash
# 启用 A2A
export A2A_ENABLED=true
mvn spring-boot:run

# 查看 AgentCard
curl http://localhost:8080/a2a/card

# 本地测试对话
curl "http://localhost:8080/api/demo19/ask?message=你好"
```

## 4. 为什么 A2A 是前沿方向？

- **跨框架互操作**：Spring AI Agent 可以和 LangChain / AgentScope / AutoGen Agent 直接对话
- **企业级多 Agent 系统**：不同团队的 Agent 用不同框架，A2A 是统一通信层
- **与 MCP 互补**：MCP 解决"Agent 用工具"，A2A 解决"Agent 用 Agent"

| 生产建议 | 说明 |
|----------|------|
| AgentCard 描述要精确 | 其他 Agent 靠描述决定要不要调用你 |
| 技能粒度适中 | 太粗不好复用，太细增加协调成本 |
| 认证鉴权 | 生产环境 A2A 端点需要身份验证 |
| 超时与重试 | Agent 间调用可能慢，需要熔断保护 |

## 5. 小结

- A2A 是 Agent 间的"外交通道"，与 MCP（工具接口）互补
- AgentCard 是发现机制，JSON-RPC 是通信格式
- Spring AI 社区提供了 A2A 服务端自动配置
- demo19 演示了如何让一个 Spring AI Agent 同时支持本地对话和 A2A 远程调用
- **MCP + A2A = Agent 的完整连接能力**（对内用工具，对外用 Agent）

→ 下一篇：[12 · 电商售后实战](/12-ecommerce-after-sales)
