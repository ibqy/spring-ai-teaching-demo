# Spring AI 教学演示项目

> 面向初学者的 Spring AI 渐进式教学项目 · 作者：ibqy · 日期：2026-09-10

本项目是参考 `mybatis-plus-demo` 的工程规范整理的教学型示例，采用 **demo01 ~ demo14** 增量式教学法：`demo01 ~ demo08` 讲核心能力，`demo09 ~ demo14` 做进阶与实战组合。每个 Demo 用最少的代码讲清一个 Spring AI 核心知识点，代码全部带中文注释，配套一份可交互的《教学演示指南》HTML 文档。

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Spring Boot | 4.1.0 | 基础框架，自动配置 |
| Spring AI | 2.0.1 | AI 应用开发框架（BOM 统一管理） |
| Java | 21 | 官方要求版本 |
| OpenAI 启动器 | 2.0.1 | 提供 ChatModel / EmbeddingModel |
| Vector Store | 2.0.1 | demo08/demo13 RAG 使用内存向量库 |
| Tika 文档读取器 | 2.0.1 | demo13 ETL 用 Tika 解析 PDF / Word 等文档 |
| Agent Skills 工具库 | 0.12.0 | demo14 用 SkillsTool / FileSystemTools / ShellTools 实现智能体技能 |

## 快速开始

### 1. 配置环境变量

项目通过环境变量读取模型配置（对接的是 **OpenAI 兼容协议**，DeepSeek / 智谱 / 通义千问 / 本机 Ollama 都适用）：

```bash
# Linux / macOS
export OPENAI_API_KEY=你的APIKey
export OPENAI_BASE_URL=https://api.openai.com        # 换成兼容服务地址即可
export OPENAI_MODEL=gpt-4o-mini
export OPENAI_EMBEDDING_MODEL=text-embedding-3-small # demo08/demo13 用到
```

### 2. 启动

```bash
mvn spring-boot:run      # 或直接运行 SpringAiTeachingDemoApplication
```

### 3. 访问 Demo

服务默认跑在 `http://localhost:8080`，所有接口都是 `GET`，浏览器直接访问即可。

| Demo | 接口 | 知识点 |
| --- | --- | --- |
| demo01 | `/api/demo01/chat?message=你好` | ChatClient 基础对话 |
| demo02 | `/api/demo02/chat-stream?message=写一首短诗` | 流式对话（Server-Sent Events） |
| demo03 | `/api/demo03/actor?actor=周星驰` | 结构化输出（entity） |
| demo04 | `/api/demo04/poem?topic=春天` | 提示词模板（{变量}） |
| demo05 | `/api/demo05/weather?city=杭州` | 函数调用（@Tool） |
| demo06 | `/api/demo06/chat?conversationId=xb&message=我的名字是小北` | 会话记忆（Advisor） |
| demo07 | `/api/demo07/advice?role=Java高级工程师&question=如何学习Spring AI` | 系统角色（System Message） |
| demo08 | `/api/demo08/ask?question=营业时间是什么` | RAG 检索增强生成 |
| demo09 | `/api/demo09/orders?text=帮我查一下 A1001 的订单` | 工具自动注册（ToolCallbackProvider 一键打包） |
| demo10 | `/api/demo10/parse?text=…订单文本…` | 批量结构化输出（List + ParameterizedTypeReference） |
| demo11 | `POST /api/demo11/ask`（JSON：`{"conversationId":"xb","message":"退货运费谁出"}`） | 实战·组合：记忆+人设+知识库+工具的多轮智能客服 |
| demo12 | `/api/demo12/chat?message=你好`（控制台观察日志） | 自定义 Advisor 扩展点（日志/耗时统计） |
| demo13 | `/api/demo13/ask?question=会员积分怎么算` | ETL 文档管道：把 PDF / Word 等文档接入知识库 |
| demo14 | `/api/demo14/ask?question=运行脚本统计一下知识库` | Agent Skills：让 Skill 读取文档、执行脚本 |

> demo06 需要先记入信息再询问，才能看到"记忆"效果；demo05 和 demo08 是最能体现 AI 应用落地的两个例子，建议重点演示。进阶阶段推荐逐个跑 demo09→demo10→demo12→demo13→demo14，最后用 demo11 验收全部组合能力。

## 项目结构

```
spring-ai-teaching-demo/
├── docs/
│   └── spring-ai-teaching-guide/      # 教学演示指南（HTML，可交互、可打印）
├── src/
│   └── main/
│       ├── java/com/xb/springai/
│       │   ├── SpringAiTeachingDemoApplication.java  # 启动类
│       │   └── controller/
│       │       ├── demo01/  … 基础对话
│       │       ├── demo02/  … 流式对话
│       │       ├── demo03/  … 结构化输出（ActorFilm record）
│       │       ├── demo04/  … 提示词模板
│       │       ├── demo05/  … 函数调用（WeatherTools 工具）
│       │       ├── demo06/  … 会话记忆
│       │       ├── demo07/  … 系统角色
│       │       ├── demo08/  … RAG（RagConfig + 向量库）
│       │       ├── demo09/  … 工具自动注册（OrderTools + MethodToolCallbackProvider）
│       │       ├── demo10/  … 批量结构化输出（List + ParameterizedTypeReference）
│       │       ├── demo11/  … 实战·多轮智能客服（AfterSalesTool + AskRequest）
│       │       ├── demo12/  … 自定义 Advisor（Demo12LoggingAdvisor）
│       │       ├── demo13/  … ETL 文档管道（EtlConfig + Tika 读取器）
│       │       └── demo14/  … Agent Skills（SkillsTool + FileSystemTools + ShellTools）
│       └── resources/
│           ├── application.yml       # OpenAI 兼容配置
│           ├── kb/
│           │   ├── shop-intro.txt    # demo08/demo11 的私有知识库资料
│           │   └── shop-policy.md    # demo13 ETL 内置示例文档（放 PDF/DOCX 亦可）
│           └── skills/
│               ├── kb-reader/SKILL.md       # demo14 技能①：读取门店知识库文档
│               └── script-runner/SKILL.md   # demo14 技能②：执行项目内脚本
├── scripts/
│   └── report.sh                    # demo14 script-runner 技能调用的示例脚本
└── pom.xml                           # 依赖与 BOM 管理
```

## 教学文档

`docs/spring-ai-teaching-guide/spring-ai-teaching-guide.html` 是一份可直接打开的图文教学指南，包含：

- 为什么用 Spring AI（与传统手写方式对比）
- 项目搭建与依赖配置
- 十四个 Demo 的 **学习依赖路线图**（流程图）
- 每个 Demo 的核心代码、讲解与运行示例
- 函数调用 **时序图**、RAG **完整链路图**、Advisor **扩展点拆解**（Mermaid 可视化）
- **术语表**：Spring AI 关键名词速查
- **实战工作坊**：从 Demo 到真实项目（客服机器人接数据库/KB 的上线路径）

> 打开方式：直接用浏览器打开该 HTML 文件即可，支持打印成 PDF 用于课堂教学。

### Markdown 学习文档

| 文档 | 内容 |
|------|------|
| [01-快速开始](docs/01-quick-start.md) | 环境准备、API Key 配置、第一个 demo |
| [02-核心概念](docs/02-core-concepts.md) | ChatClient/ChatModel/Prompt/Tool/VectorStore |
| [03-RAG 深入](docs/03-rag-deep-dive.md) | 文档加载→切分→向量化→检索 完整链路 |
| [04-函数调用](docs/04-tool-calling-guide.md) | @Tool 注解、ToolCallback、参数校验 |
| [05-Agent Skills](docs/05-agent-skills-guide.md) | SkillsTool/FileSystemTools/ShellTools 三件套 |
| [06-ETL 管道](docs/06-etl-pipeline-guide.md) | TikaDocumentReader→TokenTextSplitter→VectorStore |
| [07-Advisor 扩展](docs/07-advisor-guide.md) | 自定义 Advisor 日志/耗时统计 |
| [08-结构化输出](docs/08-structured-output-guide.md) | @JsonClassDescription 实体映射+校验 |
| [09-生产部署](docs/09-production-checklist.md) | 安全/多租户/可观测/限流熔断 |

## 测试

`src/test/.../demo05/WeatherToolsTest` 与 `src/test/.../demo10/Demo10OrderTest` 提供了**不依赖真实大模型、可离线运行**的单元测试示例（逻辑与 JSON 映射单元测试）：

```bash
mvn test
```

## 说明

- 所有代码注释与文档均以教学为目的，作者署名皆为 **xb**。
- `application.yml` 中的 API Key 默认占位为 `demo`，请务必通过环境变量注入真实密钥，切勿提交到仓库（`.gitignore` 已配置）。
- demo、接口路径、配置文件均做了详细的中文注释，方便对照教学文档学习。
