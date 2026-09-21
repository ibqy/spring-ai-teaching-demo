# Spring AI 教学演示项目

> 面向初学者的 Spring AI 渐进式教学项目 · 作者：ibqy · 日期：2026-09-10

本项目是参考 `mybatis-plus-demo` 的工程规范整理的教学型示例，采用 **demo01 ~ demo18** 增量式教学法：`demo01 ~ demo08` 讲核心能力，`demo09 ~ demo15` 做进阶与实战组合，`demo16 ~ demo17` 追前沿（MCP 协议、多模态），`demo18` 综合实战（Tool + Skill + RAG + MCP 构建电商售后助手）。每个 Demo 用最少的代码讲清一个 Spring AI 核心知识点，代码全部带中文注释，配套一份可交互的《教学演示指南》HTML 文档。

## 技术栈

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Spring Boot | 4.1.0 | 基础框架，自动配置 |
| Spring AI | 2.0.1 | AI 应用开发框架（BOM 统一管理） |
| Java | 21 | 官方要求版本 |
| OpenAI 启动器 | 2.0.1 | 提供 ChatModel / EmbeddingModel / ImageModel |
| Vector Store | 2.0.1 | demo08/demo13 RAG 使用内存向量库 |
| Tika 文档读取器 | 2.0.1 | demo13 ETL 用 Tika 解析 PDF / Word 等文档 |
| Agent Skills 工具库 | 0.12.0 | demo14 用 SkillsTool / FileSystemTools / ShellTools 实现智能体技能 |
| Nacos 客户端 | 2.4.0 | demo15 用配置中心做技能指令热更新与回滚 |
| MCP 客户端 | 2.0.1 | demo16 接入 Model Context Protocol 外部工具服务 |

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
| demo15 | `/api/demo15/ask?question=会员积分怎么算` | Nacos 配置中心：技能指令热更新与回滚（需 Nacos） |
| demo16 | `/api/demo16/ask?question=查看当前项目目录下有哪些文件` | MCP 客户端：接入 Model Context Protocol 外部工具（需 Node.js） |
| demo17 | `/api/demo17/vision?imageUrl=…&question=…` | 多模态：视觉理解 + 图片生成 |
| demo18 | `/api/demo18/chat?question=怎么申请退货？` | 综合实战：Tool + Skill + RAG + MCP 电商售后助手 |

> demo06 需要先记入信息再询问，才能看到"记忆"效果；demo05 和 demo08 是最能体现 AI 应用落地的两个例子，建议重点演示。进阶阶段推荐逐个跑 demo09→demo10→demo12→demo13→demo14→demo15→demo16→demo17，最后用 demo11 验收全部组合能力，用 demo18 体验真实场景的综合实战。

## 项目结构

```
spring-ai-teaching-demo/
├── docs/
│   └── spring-ai-teaching-guide/      # 教学演示指南（HTML，可交互、可打印）
├── src/
│   └── main/
│       ├── java/com/xb/springai/
│       │   ├── SpringAiTeachingDemoApplication.java  # 启动类
│       │   ├── common/
│       │   │   ├── ApiResponse.java                  # 统一响应封装
│       │   │   └── GlobalExceptionHandler.java       # 全局异常处理
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
│       │       ├── demo14/  … Agent Skills（SkillsTool + FileSystemTools + ShellTools）
│       │       ├── demo15/  … Agent Skills × Nacos 配置热更新与回滚
│       │       ├── demo16/  … MCP 客户端（spring-ai-starter-mcp-client）
│       │       ├── demo17/  … 多模态（视觉理解 + 图片生成）
│       │       └── demo18/  … 综合实战·电商售后助手（Tool + Skill + RAG + MCP）
│       └── resources/
│           ├── application.yml       # OpenAI 兼容配置 + MCP 客户端配置
│           ├── kb/
│           │   ├── shop-intro.txt    # demo08/demo11 的私有知识库资料
│           │   └── shop-policy.md    # demo13 ETL 内置示例文档（放 PDF/DOCX 亦可）
│           ├── docs/
│           │   └── after-sales-rules.md  # demo18 RAG 售后规则文档
│           └── skills/
│               ├── kb-reader/SKILL.md       # demo14 技能①：读取门店知识库文档
│               ├── script-runner/SKILL.md   # demo14 技能②：执行项目内脚本
│               └── after-sales/SKILL.md     # demo18 技能③：电商售后流程
├── scripts/
│   └── report.sh                    # demo14 script-runner 技能调用的示例脚本
└── pom.xml                           # 依赖与 BOM 管理
```

## 教学文档

`docs/spring-ai-teaching-guide/spring-ai-teaching-guide.html` 是一份可直接打开的图文教学指南，包含：

- 为什么用 Spring AI（与传统手写方式对比）
- 项目搭建与依赖配置
- 十七个 Demo 的 **学习依赖路线图**（流程图）
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
| [10-MCP 客户端](docs/10-mcp-guide.md) | MCP 协议、stdio/SSE、Spring AI 接入外部工具 |
| [11-多模态](docs/11-multimodal-guide.md) | 视觉理解 Media、ImageModel 图片生成 |
| [12-电商售后实战](docs/12-ecommerce-after-sales.md) | Tool + Skill + RAG + MCP 综合实战 |

## 测试

`src/test/.../` 提供了**不依赖真实大模型、可离线运行**的单元测试示例：

```
WeatherToolsTest ................ 2 tests
  ├─ 已知城市天气查询
  └─ 未知城市兜底

Demo10OrderTest ................. 2 tests
  ├─ 默认列表为空
  └─ record 字段访问

OrderToolsTest .................. 7 tests
  ├─ 订单查询 (4)
  ├─ 退换货政策 (2)
  └─ 全量遍历 (1)

AfterSalesToolTest .............. 6 tests
  ├─ 已知品类政策 (3)
  ├─ 未知品类处理 (2)
  └─ 全量遍历 (1)

GlobalExceptionHandlerTest ...... 6 tests
  ├─ ApiResponse 工厂方法 (3)
  ├─ 缺失参数/非法参数/未知异常 (3)

Total: 23 tests
```

```bash
mvn test
```

> 逻辑与 JSON 映射单元测试：把"模型无关"的纯逻辑抽出来单独测，依赖模型的链路则用 mock / 集成测试覆盖。

## 实现边界

### 已实现

- demo01~demo08：核心能力（对话/流式/结构化/模板/工具/记忆/角色/RAG）
- demo09~demo10：工具自动注册 + 批量结构化输出
- demo11：多轮智能客服（记忆+人设+知识库+工具组合）
- demo12~demo13：Advisor 扩展 + ETL 文档管道
- demo14~demo15：Agent Skills + Nacos 配置热更新
- demo16~demo17：MCP 协议 + 多模态
- demo18：综合实战·电商售后助手（Tool + Skill + RAG + MCP 协同）
- 全局异常处理：`@RestControllerAdvice` 统一错误响应 + requestId 追踪
- 参数校验：`@Valid` + `@NotBlank` 对 POST 请求体校验，`MissingServletRequestParameterException` 自动提示缺失参数

### 教学简化

- 向量库使用内存实现，生产环境应替换为 Redis / Milvus / PGVector
- 工具类使用模拟数据，真实场景应对接数据库/外部 API
- 未配置日志、监控、限流等生产级能力

### 未实现（生产环境需补充）

- 对话持久化与分布式会话
- 多模型切换与负载均衡
- 完整的权限认证与审计
- 向量库的增量更新与清理策略
- 工具调用的重试与熔断机制

## 说明

- 所有代码注释与文档均以教学为目的，作者署名皆为 **xb**。
- `application.yml` 中的 API Key 默认占位为 `demo`，请务必通过环境变量注入真实密钥，切勿提交到仓库（`.gitignore` 已配置）。
- demo、接口路径、配置文件均做了详细的中文注释，方便对照教学文档学习。
