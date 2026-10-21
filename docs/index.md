---
layout: home

hero:
  name: Spring AI 教学实战
  text: 从入门到生产的教学项目
  tagline: 21 个渐进式 Demo 覆盖 Chat、RAG、工具调用、MCP（客户端+服务端）、多模态、A2A 智能体通信、Advisor、结构化输出、Agent Skills、Token 可观测 —— 从单轮对话到电商售后综合实战
  actions:
    - theme: brand
      text: 开始学习 →
      link: /01-quick-start
    - theme: alt
      text: GitHub 源码
      link: https://github.com/ibqy/spring-ai-teaching-demo

features:
  - icon: 🚀
    title: 快速开始
    details: 环境准备、首个 ChatClient 调用、项目结构导览，10 分钟跑通第一个 AI 应用
  - icon: 🧠
    title: 核心概念
    details: ChatClient / ChatModel / Advisor 职责边界与调用链路
  - icon: 📚
    title: RAG 深入与 ETL 管道
    details: 文档读取、切分、向量化、检索增强的完整链路实现
  - icon: 🔧
    title: 工具调用与 MCP
    details: Function Calling 注册与执行，MCP 客户端接入外部工具、服务端发布自有工具
  - icon: 🎭
    title: Agent Skills 与热更新
    details: Agent Skills 技能封装，Nacos 配置热更新实战
  - icon: 📋
    title: 结构化输出
    details: 大模型输出到强类型 Java 对象的映射与容错
  - icon: 📊
    title: Token 用量可观测
    details: 用 Advisor 做 Token 计量与成本估算，给 AI 应用装上电表
  - icon: ✅
    title: 生产检查清单
    details: 超时、重试、降级、成本控制——上线前逐项核对
  - icon: 🛒
    title: 电商售后综合实战
    details: demo18 综合运用 Tool + Skill + RAG + MCP 构建真实电商售后 AI 助手
  - icon: 🔗
    title: A2A 智能体通信
    details: demo19 接入 Agent-to-Agent 协议，让不同框架的 Agent 互相发现和调用
  - icon: 🌐
    title: 完整 HTML 教程
    details: 图文版教学指南（GitHub 目录），配合文档站对照阅读
---

## 课程结构

| 章节 | 主题 | 对应代码 |
|------|------|----------|
| [01 · 快速开始](/01-quick-start) | 环境、API Key、首个对话与流式体验 | [Demo01BasicChatController](../src/main/java/com/xb/springai/controller/demo01/Demo01BasicChatController.java) · [Demo02StreamingChatController](../src/main/java/com/xb/springai/controller/demo02/Demo02StreamingChatController.java) |
| [02 · 核心概念](/02-core-concepts) | ChatClient / ChatModel / Prompt / Tool / VectorStore / Advisor 职责边界 | [Demo01BasicChatController](../src/main/java/com/xb/springai/controller/demo01/Demo01BasicChatController.java) · [RagConfig](../src/main/java/com/xb/springai/controller/demo08/RagConfig.java) · [Demo12LoggingAdvisor](../src/main/java/com/xb/springai/controller/demo12/Demo12LoggingAdvisor.java) |
| [03 · RAG 深入](/03-rag-deep-dive) | 手写 RAG（demo08） vs 标准 ETL（demo13） | [Demo08RagController](../src/main/java/com/xb/springai/controller/demo08/Demo08RagController.java) · [RagConfig](../src/main/java/com/xb/springai/controller/demo08/RagConfig.java) |
| [04 · 函数调用](/04-tool-calling-guide) | `@Tool`、`ToolCallbackProvider` 批量注册、returnDirect | [Demo05FunctionCallingController](../src/main/java/com/xb/springai/controller/demo05/Demo05FunctionCallingController.java) · [Demo09ToolCallingController](../src/main/java/com/xb/springai/controller/demo09/Demo09ToolCallingController.java) |
| [05 · Agent Skills](/05-agent-skills-guide) | SkillsTool / FileSystemTools / ShellTools 三件套 | [Demo14AgentSkillsController](../src/main/java/com/xb/springai/controller/demo14/Demo14AgentSkillsController.java) |
| [06 · ETL 管道](/06-etl-pipeline-guide) | TikaDocumentReader → TokenTextSplitter → VectorStore | [Demo13EtlController](../src/main/java/com/xb/springai/controller/demo13/Demo13EtlController.java) · [EtlConfig](../src/main/java/com/xb/springai/controller/demo13/EtlConfig.java) |
| [07 · Advisor 扩展点](/07-advisor-guide) | 日志/耗时、内容过滤、成本、缓存四类 Advisor | [Demo12LoggingAdvisor](../src/main/java/com/xb/springai/controller/demo12/Demo12LoggingAdvisor.java) · [Demo12AdvisorController](../src/main/java/com/xb/springai/controller/demo12/Demo12AdvisorController.java) |
| [08 · 结构化输出](/08-structured-output-guide) | 单对象与批量列表映射、泛型与容错 | [Demo03StructuredOutputController](../src/main/java/com/xb/springai/controller/demo03/Demo03StructuredOutputController.java) · [Demo10ListStructuredOutputController](../src/main/java/com/xb/springai/controller/demo10/Demo10ListStructuredOutputController.java) |
| [09 · 生产部署清单](/09-production-checklist) | 安全、多租户、可观测、限流、重试 | [GlobalExceptionHandler](../src/main/java/com/xb/springai/common/GlobalExceptionHandler.java) · [ApiResponse](../src/main/java/com/xb/springai/common/ApiResponse.java) |
| [10 · MCP 客户端](/10-mcp-guide) | 接入外部 MCP Server 的工具 | [Demo16McpClientController](../src/main/java/com/xb/springai/controller/demo16/Demo16McpClientController.java) |
| [11 · 多模态](/11-multimodal-guide) | 图片理解（Media）与图片生成（ImageModel） | [Demo17MultimodalController](../src/main/java/com/xb/springai/controller/demo17/Demo17MultimodalController.java) |
| [12 · 电商售后综合实战](/12-ecommerce-after-sales) | Tool + Skill + RAG + MCP 四能力协同 | [Demo18EcommerceAfterSalesController](../src/main/java/com/xb/springai/controller/demo18/Demo18EcommerceAfterSalesController.java) |
| [13 · A2A 智能体通信](/13-a2a-guide) | AgentCard、JSON-RPC、跨框架互调 | [Demo19A2aController](../src/main/java/com/xb/springai/controller/demo19/Demo19A2aController.java) |
| [14 · MCP 服务端](/14-mcp-server-guide) | 把自己的工具发布成 MCP 服务 | [Demo20McpServerConfig](../src/main/java/com/xb/springai/controller/demo20/Demo20McpServerConfig.java) · [Demo20ShopTools](../src/main/java/com/xb/springai/controller/demo20/Demo20ShopTools.java) |
| [15 · Token 用量可观测](/15-observability-guide) | Advisor 计量、LongAdder、成本估算 | [Demo21TokenAccountingAdvisor](../src/main/java/com/xb/springai/controller/demo21/Demo21TokenAccountingAdvisor.java) · [Demo21UsageController](../src/main/java/com/xb/springai/controller/demo21/Demo21UsageController.java) |

## 代码地图

| 包 | 关键类 | 职责 |
|----|--------|------|
| 根包 | [SpringAiTeachingDemoApplication](../src/main/java/com/xb/springai/SpringAiTeachingDemoApplication.java) | 启动入口 |
| `common` | [ApiResponse](../src/main/java/com/xb/springai/common/ApiResponse.java) / [GlobalExceptionHandler](../src/main/java/com/xb/springai/common/GlobalExceptionHandler.java) | 统一响应包装与模型异常兜底（超时/401/限流） |
| `controller/demo01`–`demo04` | [Demo01BasicChatController](../src/main/java/com/xb/springai/controller/demo01/Demo01BasicChatController.java) / [Demo02StreamingChatController](../src/main/java/com/xb/springai/controller/demo02/Demo02StreamingChatController.java) / [Demo03StructuredOutputController](../src/main/java/com/xb/springai/controller/demo03/Demo03StructuredOutputController.java) / [Demo04PromptTemplateController](../src/main/java/com/xb/springai/controller/demo04/Demo04PromptTemplateController.java) | ChatClient 基础四件套：同步、流式、`.entity()` 映射、提示词模板 |
| `controller/demo05`–`demo08` | [Demo05FunctionCallingController](../src/main/java/com/xb/springai/controller/demo05/Demo05FunctionCallingController.java) / [Demo06ChatMemoryController](../src/main/java/com/xb/springai/controller/demo06/Demo06ChatMemoryController.java) / [Demo07SystemRoleController](../src/main/java/com/xb/springai/controller/demo07/Demo07SystemRoleController.java) / [Demo08RagController](../src/main/java/com/xb/springai/controller/demo08/Demo08RagController.java) | 工具调用、记忆隔离、动态角色、手写 RAG（含 [RagConfig](../src/main/java/com/xb/springai/controller/demo08/RagConfig.java)） |
| `controller/demo09`–`demo11` | [Demo09ToolCallingController](../src/main/java/com/xb/springai/controller/demo09/Demo09ToolCallingController.java) / [Demo10ListStructuredOutputController](../src/main/java/com/xb/springai/controller/demo10/Demo10ListStructuredOutputController.java) / [Demo11CustomerServiceController](../src/main/java/com/xb/springai/controller/demo11/Demo11CustomerServiceController.java) | 工具批量注册、批量列表输出、工具+记忆的综合客服 |
| `controller/demo12`–`demo13` | [Demo12LoggingAdvisor](../src/main/java/com/xb/springai/controller/demo12/Demo12LoggingAdvisor.java) / [Demo13EtlController](../src/main/java/com/xb/springai/controller/demo13/Demo13EtlController.java) / [EtlConfig](../src/main/java/com/xb/springai/controller/demo13/EtlConfig.java) | 自定义 Advisor 实现范式、Tika ETL 管道 |
| `controller/demo14`–`demo15` | [Demo14AgentSkillsController](../src/main/java/com/xb/springai/controller/demo14/Demo14AgentSkillsController.java) / [Demo15NacosSkillsController](../src/main/java/com/xb/springai/controller/demo15/Demo15NacosSkillsController.java) / [NacosSkillConfigManager](../src/main/java/com/xb/springai/controller/demo15/NacosSkillConfigManager.java) | Agent Skills 三件套、Nacos 配置热更新技能 |
| `controller/demo16`–`demo17` | [Demo16McpClientController](../src/main/java/com/xb/springai/controller/demo16/Demo16McpClientController.java) / [Demo17MultimodalController](../src/main/java/com/xb/springai/controller/demo17/Demo17MultimodalController.java) | MCP 客户端（工具清单握手）、多模态（Media / ImageModel） |
| `controller/demo18` | [Demo18EcommerceAfterSalesController](../src/main/java/com/xb/springai/controller/demo18/Demo18EcommerceAfterSalesController.java)（419 行） | 全课程收官：Tool + Skill + RAG + MCP 四能力协同 |
| `controller/demo19`–`demo20` | [Demo19A2aController](../src/main/java/com/xb/springai/controller/demo19/Demo19A2aController.java) / [Demo20McpServerConfig](../src/main/java/com/xb/springai/controller/demo20/Demo20McpServerConfig.java) / [Demo20ShopTools](../src/main/java/com/xb/springai/controller/demo20/Demo20ShopTools.java) | A2A 智能体互调、MCP 服务端发布 |
| `controller/demo21` | [Demo21TokenAccountingAdvisor](../src/main/java/com/xb/springai/controller/demo21/Demo21TokenAccountingAdvisor.java) / [Demo21UsageController](../src/main/java/com/xb/springai/controller/demo21/Demo21UsageController.java) | Token 计量 Advisor + 用量/成本查询端点 |

## 在路线中的位置

> **Java × AI 进阶路线** · 阶段 2「AI 应用工程入门」第 1 站（建议第一站）  
> **Spring AI 主线** → 下一站：[Spring AI Alibaba（通义千问生态）](https://ibqy.github.io/spring-ai-alibaba-teaching-demo/)  
> 完整路线：[ibqy.github.io/curriculum](https://ibqy.github.io/curriculum)
