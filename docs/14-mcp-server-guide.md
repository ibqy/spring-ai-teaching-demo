# 14 · MCP 服务端实战

> **前置 Demo**：demo05（@Tool 函数调用）、demo09（ToolCallbackProvider 工具打包）、demo16（MCP 客户端）
> **涉及 Demo**：demo20  
> **代码**：[Demo20McpServerConfig](../src/main/java/com/xb/springai/controller/demo20/Demo20McpServerConfig.java) · [Demo20McpServerController](../src/main/java/com/xb/springai/controller/demo20/Demo20McpServerController.java) · [Demo20ShopTools](../src/main/java/com/xb/springai/controller/demo20/Demo20ShopTools.java)
> **目标**：理解 MCP 的 Server 角色，把自己的工具按 MCP 协议发布出去，实现「一次发布、任意客户端可用」。

---

## 1. 为什么需要 MCP 服务端？

demo16 演示的是 MCP **客户端**：我们的应用去连接别人发布的 MCP 服务器（文件系统服务器），消费它的工具。

但真实项目里，你更常见的需求是反过来的：

> 「我们公司的订单系统有一套业务工具，能不能让**任何** AI 应用（Claude Desktop、Cursor、别的团队的 Spring AI 服务）都能调用，而不用每家对接一遍？」

这就是 MCP **服务端**要解决的问题：**把工具发布成标准协议服务**。

```
┌─────────────┐    MCP 协议（/mcp 端点）    ┌─────────────────────┐
│ 任意 MCP 客户端 │  ◄────────────────────  │ 你的 Spring Boot 应用  │
│ (Claude/Inspector│   initialize → tools/list│  （MCP Server）        │
│  /其他 Spring AI)│  → tools/call          │  暴露 queryOrder 等工具 │
└─────────────┘                          └─────────────────────┘
```

**核心价值**：工具实现写一次（Spring Bean + `@Tool`），协议转换由框架自动完成，客户端零适配。

---

## 2. 三步发布一个 MCP 服务

demo20 的全部改动只有三步，业务代码完全不碰协议：

### 步骤一：加依赖（pom.xml）

```xml
<!-- Servlet（Tomcat）栈用 webmvc 变体；WebFlux 栈用 spring-ai-starter-mcp-server-webflux -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

### 步骤二：把工具 Bean 声明成 ToolCallbackProvider Bean

```java
@Configuration
public class Demo20McpServerConfig {
    @Bean
    public ToolCallbackProvider demo20ShopToolProvider(Demo20ShopTools shopTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(shopTools)   // 扫描 Bean 内所有 @Tool 方法
                .build();
    }
}
```

> 引入 starter 后，Spring AI 的 `ToolCallbackConverterAutoConfiguration` 会自动收集容器内**所有** `ToolCallbackProvider` Bean，转换成 MCP 工具规格（tool specifications）发布出去。这就是「零协议编码」的秘密。

### 步骤三：配置端点（application.yml）

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true                    # 开关（默认就是 true）
        name: spring-ai-teaching-demo    # 服务器名称（客户端握手时可见）
        version: 1.0.0
        protocol: streamable             # 传输协议：streamable / sse / stateless
        type: SYNC                       # SYNC = 同步服务端（Servlet 栈标准选择）
        streamable-http:
          mcp-endpoint: /mcp             # MCP 端点路径
```

启动应用，`POST /mcp` 就是一个标准 MCP 服务器了。

---

## 3. 动手验证：curl 四步曲

MCP streamable HTTP 是**有会话状态**的协议，手工验证必须按顺序走四步：

### ① initialize 握手

```bash
curl -i -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"1.0"}}}'
```

**关键**：响应头里会带回 `mcp-session-id`（每次握手都不同），后面三步必须带上它，否则报 `Session ID missing`。

### ② 发送 initialized 通知

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "mcp-session-id: <上一步拿到的 SESSION_ID>" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}'
```

通知（notification）没有 `id`，服务器不回包——这是 JSON-RPC 的规范。

### ③ tools/list 看工具

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "mcp-session-id: <SESSION_ID>" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
```

返回 SSE 事件流，取 `data:` 行就是 JSON。能看到 demo20 发布的三个工具及其输入 JSON Schema：

```json
{"tools":[
  {"name":"queryOrder","description":"根据订单号查询订单的最新状态……",
   "inputSchema":{"type":"object","properties":{"orderId":{"type":"string"}},"required":["orderId"]}},
  {"name":"queryLogistics","description":"根据订单号查询物流轨迹……","inputSchema":{...}},
  {"name":"getRefundPolicy","description":"查询小店标准退换货规则……","inputSchema":{...}}
]}
```

### ④ tools/call 调用工具

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "mcp-session-id: <SESSION_ID>" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"queryOrder","arguments":{"orderId":"A1001"}}}'
```

返回：

```json
{"result":{"content":[{"type":"text","text":"\"已发货，金额 299 元\""}],"isError":false}}
```

`isError=false` 且 `text` 是 `Demo20ShopTools.queryOrder("A1001")` 的真实返回值——**你的工具已经被远程客户端通过标准协议调用了**。

> 嫌 curl 麻烦？项目自带两个便捷入口：
> - `GET /api/demo20/status`：浏览器直接看已发布的工具清单
> - `GET /api/demo20/inspector`：拿 MCP Inspector 配置与完整 curl 步骤
>
> 图形化调试用官方 MCP Inspector：
> ```bash
> npx @modelcontextprotocol/inspector
> # Transport 选 Streamable HTTP，URL 填 http://localhost:8080/mcp
> ```

---

## 4. 协议流程全景

```
客户端                               服务端（你的应用）
  │ ── initialize ────────────────► │  协商协议版本，创建会话
  │ ◄──── result + mcp-session-id ──│  （后续请求都要带 session id）
  │ ── notifications/initialized ──► │  通知：握手完成（无响应）
  │ ── tools/list ─────────────────► │  返回工具清单（name/description/inputSchema）
  │ ◄──── result（SSE 事件流）────────│
  │ ── tools/call {name, arguments} ► │  执行对应 @Tool 方法
  │ ◄──── result {content, isError} ─│  返回执行结果
```

对比 demo16 的客户端角色：demo16 是「我们的应用发起 initialize，调用别人的工具」；demo20 是「别人的客户端向我们发起 initialize，调用我们的工具」。**同一套协议，两个方向。**

---

## 5. 与 demo16 客户端互通的「自连」演练

本应用同时具备客户端（demo16，默认关闭）和服务端（demo20，默认开启）能力。想体验「服务器 → 客户端 → 模型」的完整闭环，可以把 demo16 的客户端指向自己的服务端：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        streamable-http:                 # streamable HTTP 传输的客户端配置
          servers:
            self:
              url: http://localhost:8080/mcp   # 指向本应用自己的 MCP 端点
      # stdio:
      #   servers: ...                   # 原来的 npx 文件系统服务器配置
```

然后访问 `/api/demo16/ask?question=查一下订单A1001的状态`——模型会通过 MCP 客户端调用你自己发布的 `queryOrder` 工具。一次请求穿过「协议客户端 → HTTP → 协议服务端 → Spring Bean」，这就是微服务架构下 AI 工具调用的标准形态。

---

## 6. 生产注意事项

| 关注点 | 建议 |
|---|---|
| **暴露面控制** | 容器内所有 `ToolCallbackProvider` Bean 都会被自动发布。敏感工具应拆分独立部署，或用网络边界（内网/网关鉴权）限制 `/mcp` 访问 |
| **工具描述质量** | `@Tool(description=...)` 是模型选择工具的唯一依据，写清楚「什么时候用」，别只写「是什么」 |
| **入参校验** | MCP 客户端可能传入任意参数，工具方法内要校验（orderId 格式、长度），返回友好错误而不是抛异常 |
| **传输选型** | streamable（推荐，单向 HTTP + SSE 响应）/ sse（旧式双向）/ stateless（无会话，适合 Serverless）。本项目 Servlet 栈选 streamable + SYNC |
| **webmvc vs webflux** | 与项目的 Web 栈匹配：Tomcat → `-server-webmvc`；Netty → `-server-webflux` |
| **版本演进** | Spring AI 1.x 的 `spring.ai.mcp.server.sse-endpoint` 等属性已废弃，2.x 统一为 `protocol` + `streamable-http.mcp-endpoint`，迁移时注意配置更名 |

---

## 7. 小结

- MCP 服务端 = 「把 `@Tool` 方法按标准协议发布」，三步搞定：加 starter → 声明 Provider Bean → 配端点
- 协议细节（会话、SSE、JSON-RPC）全部由 Spring AI 自动配置处理，业务代码零协议感知
- 验证手段：`/api/demo20/status` 看清单，curl 四步曲走协议，MCP Inspector 做图形化调试
- 进阶：demo16 客户端指向自己的 `/mcp`，完整体验「工具发布 → 协议调用 → 模型决策」闭环
