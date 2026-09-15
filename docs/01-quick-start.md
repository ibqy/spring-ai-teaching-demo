# 01 · 快速开始

> **前置 Demo**：无  
> **涉及 Demo**：demo01、demo02  
> **目标**：完成环境准备、API Key 配置，跑通第一个 Spring AI 对话。

---

## 1. 环境要求

| 项目 | 版本 / 说明 |
|------|-------------|
| JDK | 21+（Spring AI 2.0 硬性要求） |
| Maven | 3.6+ |
| IDE | IntelliJ IDEA / VS Code 均可 |
| 大模型服务 | 支持 OpenAI 兼容协议的任意服务（DeepSeek、智谱、通义千问、Ollama 等） |

---

## 2. 项目依赖

核心依赖已在 `pom.xml` 中配置好，关键条目：

```xml
<!-- Spring AI BOM 统一版本管理 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>2.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- OpenAI 兼容模型启动器（提供 ChatModel / EmbeddingModel） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

<!-- Web 支持（REST 接口演示） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

---

## 3. 配置 API Key

`application.yml` 中通过环境变量读取密钥，**不写死在配置文件里**：

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

### 设置环境变量

**Linux / macOS：**

```bash
export OPENAI_API_KEY=你的APIKey
export OPENAI_BASE_URL=https://api.openai.com        # 换成兼容服务地址即可
export OPENAI_MODEL=gpt-4o-mini
```

**Windows（PowerShell）：**

```powershell
$env:OPENAI_API_KEY="你的APIKey"
$env:OPENAI_BASE_URL="https://api.openai.com"
$env:OPENAI_MODEL="gpt-4o-mini"
```

> **兼容协议说明**：Spring AI 的 `spring-ai-starter-model-openai` 对接的是 OpenAI **兼容协议**。如果你使用 DeepSeek / 智谱 / 通义千问 / 本地 Ollama，只需把 `base-url` 换成对应地址即可，代码无需任何修改。

### 常用模型服务的 base-url

| 服务商 | base-url 示例 |
|--------|--------------|
| OpenAI | `https://api.openai.com` |
| DeepSeek | `https://api.deepseek.com` |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Ollama（本地） | `http://localhost:11434` |

---

## 4. 启动项目

```bash
# 克隆后直接运行
mvn spring-boot:run

# 或 IDE 中直接运行 SpringAiTeachingDemoApplication
```

启动成功后会看到：

```
Started SpringAiTeachingDemoApplication in X.XXX seconds
```

服务默认跑在 `http://localhost:8080`。

---

## 5. 跑通第一个 Demo

直接在浏览器访问：

```
http://localhost:8080/api/demo01/chat?message=你好
```

你会看到模型返回一段文本。这行代码的背后发生了什么？

```java
@RestController
@RequestMapping("/api/demo01")
public class Demo01BasicChatController {

    private final ChatClient chatClient;

    public Demo01BasicChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "用一句话介绍什么是 Spring AI") String message) {
        return this.chatClient.prompt()
                .user(message)   // 用户消息
                .call()          // 同步调用大模型
                .content();      // 取出回答文本
    }
}
```

三步完成一次对话：

1. `.prompt().user(message)` — 构造请求，把用户输入作为"用户消息"
2. `.call()` — 同步调用大模型（阻塞等待返回）
3. `.content()` — 取出模型返回的文本内容

---

## 6. 体验流式对话

再试试 demo02 的"打字机效果"：

```
http://localhost:8080/api/demo02/chat-stream?message=写一首关于编程的短诗
```

浏览器会逐片段接收文本，体验和 ChatGPT 的打字效果一模一样。实现只需把 `.call()` 换 `.stream()`：

```java
@GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String message) {
    return this.chatClient.prompt()
            .user(message)
            .stream()       // 流式调用
            .content();     // 返回 Flux<String> 数据流
}
```

核心差异：

| 方式 | 方法 | 返回类型 | 体验 |
|------|------|----------|------|
| 同步 | `.call()` | 最终字符串 | 全部生成完一次性返回 |
| 流式 | `.stream()` | `Flux<String>` | 边生成边返回，逐字出现 |

---

## 7. 遇到问题？

| 问题 | 排查方向 |
|------|----------|
| 连接超时 / 401 | 检查 `OPENAI_API_KEY` 和 `OPENAI_BASE_URL` 是否正确 |
| 模型不存在 | 检查 `OPENAI_MODEL` 是否填写了服务商支持的模型名 |
| 无法启动 | 确认 JDK 版本 ≥ 21（`java -version` 检查） |
| 端口占用 | 修改 `server.port` 或关闭占用 8080 的进程 |

---

## 下一步

跑通基础对话后，前往 [02 · 核心概念](./02-core-concepts.md) 系统性地了解 Spring AI 的核心接口体系。
