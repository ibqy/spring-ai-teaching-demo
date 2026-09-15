# 10 · MCP 客户端实战

> **前置 Demo**：demo05、demo09（掌握函数调用基础）
> **涉及 Demo**：demo16
> **目标**：理解 MCP（Model Context Protocol），掌握 Spring AI 作为 MCP 客户端接入外部工具服务。

---

## 1. 什么是 MCP？

**MCP（Model Context Protocol，模型上下文协议）** 是 2024 年底由 Anthropic 提出、现已获 OpenAI / Google / 微软等主流厂商采纳的开放协议。它的定位是：

> AI 应用与外部工具之间的 **"USB-C 标准"** —— 以前每接一个工具（文件系统、数据库、GitHub、Slack…）都要写一套专用集成代码；有了 MCP，工具方只需实现一次 MCP Server，所有 AI 应用都能即插即用。

```
    传统方式（N×M 问题）                MCP 方式（N+M）
  ┌────────┐  ┌────────┐             ┌────────┐
  │ App A  │  │ App B  │             │ App A  │  ─┐
  └───┬────┘  └───┬────┘             └────────┘   │  MCP 协议
      │  专用代码  │                   ┌────────┐   ├────────►  ┌──────────┐
  ┌───▼────┐  ┌───▼────┐             │ App B  │  ─┘           │  MCP     │
  │ 工具 1  │  │ 工具 2  │             └────────┘              │  Server  │
  └────────┘  └────────┘                                       └──────────┘
```

---

## 2. MCP 的三个角色

| 角色 | 说明 | 本项目的扮演者 |
|------|------|----------------|
| **MCP Host** | 发起连接的应用（需要工具能力的 AI 应用） | Spring AI 应用 |
| **MCP Client** | Host 内部的协议客户端，负责握手、发现工具、调用工具 | `spring-ai-starter-mcp-client` |
| **MCP Server** | 提供工具/资源/提示词的服务端 | 官方文件系统 Server（`@modelcontextprotocol/server-filesystem`） |

**传输方式**（本项目使用 stdio）：

| 传输 | 说明 | 适用场景 |
|------|------|----------|
| **stdio** | 客户端以子进程方式启动 Server，通过标准输入输出通信 | 本地工具（文件系统、命令行） |
| **SSE** | Server 提供 HTTP 端点，客户端通过 Server-Sent Events 接收 | 远程工具（GitHub、Slack 等 SaaS） |

---

## 3. 项目中的 demo16

### 3.1 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

> WebFlux 只为 SSE 传输模式提供 WebClient；与 starter-web 共存时应用仍是 Servlet（Tomcat）。

### 3.2 配置（application.yml）

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: ${MCP_ENABLED:false}      # 默认关闭，避免无 Node.js 环境时启动失败
        stdio:
          servers:
            filesystem:                     # 自定义 Server 名
              command: npx                  # 启动命令
              args: ["-y", "@modelcontextprotocol/server-filesystem", "."]
```

启动时 Spring AI 会：

1. 以子进程方式启动 `npx @modelcontextprotocol/server-filesystem .`
2. 通过 MCP 握手协议拉取 Server 的工具清单（Read / Write / ListDirectory / GetFileInfo…）
3. 把工具打包成一个 `ToolCallbackProvider` Bean

### 3.3 代码（Demo16McpClientController）

```java
public Demo16McpClientController(
        ChatClient.Builder chatClientBuilder,
        @Autowired(required = false) ToolCallbackProvider mcpToolCallbackProvider) {

    this.mcpToolProvider = mcpToolCallbackProvider;

    if (mcpToolCallbackProvider != null) {
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(mcpToolCallbackProvider)   // 挂 MCP 工具
                .defaultSystem("你是一个接入了 MCP 工具服务的智能助手...")
                .build();
    } else {
        // 降级：未启用 MCP 时照常启动，仅提示
        this.chatClient = chatClientBuilder
                .defaultSystem("当前未接入 MCP 服务器（MCP_ENABLED=false）...")
                .build();
    }
}
```

**关键点：`@Autowired(required = false)`**。MCP 配置是"可选能力"——没有 Node.js 或未启用时，`ToolCallbackProvider` Bean 不存在。必须用可选注入，否则整个应用启动失败，影响其他 demo。

### 3.4 运行演示

```bash
# 1) 安装 Node.js（npx 随附）
# 2) 开启 MCP
export MCP_ENABLED=true

# 3) 重启后查看连接状态
curl http://localhost:8080/api/demo16/status
# → {"mcpEnabled":true,"toolCount":4,"tools":[{"name":"Read",...},{"name":"Write",...}]}

# 4) 让模型调用 MCP 工具
curl "http://localhost:8080/api/demo16/ask?question=查看当前项目目录下有哪些文件"
```

模型会先调用 MCP 的 `ListDirectory` 工具拿到真实目录结构，再组织中文回答——而不是凭空编造。

---

## 4. 为什么是"最前沿"？

- **事实标准地位**：2025 年以来，OpenAI（MCP 进入 Agents SDK）、Google（MCP 进入 Gemini）、微软（Copilot）均已原生支持 MCP，它已成为 AI 工具集成的事实标准。
- **一次实现、处处复用**：一个 MCP Server 可被 Claude、Spring AI、Cursor 等任意 Host 复用。
- **生态爆炸**：官方 servers 仓库已有文件系统、GitHub、Git、Postgres、Puppeteer、Slack 等几十个官方 Server，社区数千个。

### 生产接入建议

| 场景 | 推荐 Server / 方式 |
|------|-------------------|
| 文件系统 | `@modelcontextprotocol/server-filesystem`（限定可访问目录） |
| GitHub | `@modelcontextprotocol/server-github`（SSE/HTTP） |
| 数据库 | `@modelcontextprotocol/server-postgres`（只读查询更安全） |
| 浏览器自动化 | `@modelcontextprotocol/server-puppeteer` |
| 自研工具 | 用 `spring-ai-starter-mcp-server` 把自己的 API 包成 MCP Server |

### 安全要点

1. **最小权限**：文件系统 Server 只暴露必要目录，不要给根目录
2. **命令白名单**：stdio 模式启动的命令应固定（如 npx + 固定包名）
3. **鉴权**：SSE/HTTP 模式的远程 Server 必须走 HTTPS + Token
4. **审计**：记录 MCP 工具调用日志，便于追溯

---

## 5. 小结

- MCP 是 AI 工具集成的开放标准（Host / Client / Server 三角色）
- Spring AI 通过 `spring-ai-starter-mcp-client` 让应用秒变 MCP 客户端
- stdio 适合本地工具，SSE 适合远程 SaaS 工具
- 用 `@Autowired(required = false)` 让 MCP 成为可选能力，避免拖垮整个应用
- 生产环境务必做最小权限 + 命令白名单 + 审计

---

## 下一步

掌握 MCP 后，看看多模态（视觉理解 + 图片生成）：[11 · 多模态实战](./11-multimodal-guide.md)。
