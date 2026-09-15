# 05 · Agent Skills 实战

> **前置 Demo**：demo05、demo09（掌握函数调用基础）  
> **涉及 Demo**：demo14  
> **目标**：掌握 Agent Skills 的 SkillsTool/FileSystemTools/ShellTools 三件套，理解 SKILL.md 编写规范和渐进式披露模式。

---

## 1. 什么是 Agent Skills？

Agent Skills（智能体技能）是一种**将"能力"打包成标准化目录**的范式。它源自 Anthropic 的 Agent Skills 规范，核心理念是：

> 每个技能是一个独立目录，内含 `SKILL.md`（声明式指令）+ 可选脚本/参考文档。模型按需发现、激活、执行，无需将所有技能一次性塞进上下文。

Spring AI 2.0 通过社区库 `spring-ai-agent-utils`（0.12.0）提供了官方推荐的三件套。

---

## 2. 三件套架构

```
┌──────────────────────────────────────────────────┐
│                  ChatClient                       │
│         .defaultToolCallbacks(skillsTool)         │
│         .defaultTools(fileSystemTools, shellTools) │
├──────────────────────────────────────────────────┤
│  SkillsTool        │  FileSystemTools  │ ShellTools │
│  (技能注册/发现)     │  (读文件)          │ (执行脚本)  │
├──────────────────────────────────────────────────┤
│  扫描 SKILL.md     │  Read(dir, file)  │ Bash(cmd) │
│  渐进式披露         │  限定目录范围       │ 限定目录范围 │
└──────────────────────────────────────────────────┘
```

---

## 3. SkillsTool —— 技能注册与发现

`SkillsTool` 是三件套的**核心**。它扫描注册的技能，暴露给模型，并按需加载完整指令。

### 3.1 注册技能

```java
ToolCallback skillsTool = SkillsTool.builder()
        .addSkillsResource(new ClassPathResource("skills/kb-reader/SKILL.md"))
        .addSkillsResource(new ClassPathResource("skills/script-runner/SKILL.md"))
        .build();

this.chatClient = chatClientBuilder
        .defaultToolCallbacks(skillsTool)   // 关键：技能工具作为 toolCallback
        // ...
        .build();
```

### 3.2 SKILL.md 格式

每个技能目录下必须有一个 `SKILL.md`，格式为 YAML front matter + Markdown 指令：

```markdown
---
name: kb-reader
description: 阅读门店私有知识库（会员政策、退换售后等），回答店铺政策类问题。
---

# 门店知识库阅读

## 步骤

1. 用文件读取工具（Read）读取知识库文档，例如 `src/main/resources/kb/shop-policy.md`。
2. 从中找出与用户问题最相关的内容。
3. 基于文档内容回答；文档中未提及的信息，请直接说明"文档中未提及"，不要编造。
```

YAML front matter 的关键字段：

| 字段 | 必须 | 说明 |
|------|------|------|
| `name` | 是 | 技能唯一标识 |
| `description` | 是 | 告诉模型"什么场景该用这个技能" |

---

## 4. 渐进式披露（Progressive Disclosure）

这是 Agent Skills 最重要的设计模式。它不是把所有技能指令一次性塞进上下文窗口，而是**分阶段加载**：

```
阶段 1: Discovery（发现）
  启动时，模型仅看到每个技能的 name + description（开销极小）
  → 注册几十上百个技能也不会撑爆上下文窗口
        │
        ▼
阶段 2: Activation（激活）
  用户提问匹配到某技能的 description 时
  → 才加载该技能的完整 SKILL.md 指令
        │
        ▼
阶段 3: Execution（执行）
  按 SKILL.md 中的步骤执行：读文件、跑脚本、查 API
  → 按需执行，不预加载不必要的内容
```

**类比**：就像餐厅菜单——你不需要把每道菜的做法背下来，看到菜名 + 描述（Discovery）点菜，厨房再按菜谱（Activation）做菜（Execution）。

---

## 5. FileSystemTools —— 文件读取

`FileSystemTools` 让模型拥有**读取项目文件**的能力。它在技能执行阶段被使用，让模型可以读取知识库文档、配置文件等。

```java
.defaultTools(FileSystemTools.builder()
        .allowedDirectory(".")       // 安全边界：只允许访问项目目录
        .build())
```

实际使用示例：当 kb-reader 技能被激活后，模型使用 FileSystemTools 的 Read 工具读取 `src/main/resources/kb/shop-policy.md`，从中找到会员积分规则。

**安全建议**：

- `allowedDirectory` 应为最小必要范围（如项目目录），避免模型读取系统敏感文件
- 生产环境可进一步限制为 `kb/`、`config/` 等白名单目录

---

## 6. ShellTools —— 脚本执行

`ShellTools` 让模型拥有**执行 Shell 命令**的能力，用于运行项目内的脚本。

```java
.defaultTools(ShellTools.builder()
        .workingDirectory(".")       // 安全边界：命令只在项目目录内执行
        .build())
```

示例脚本（`scripts/report.sh`）：

```bash
#!/bin/bash
echo "===== 小北优选咖啡店 · 知识库文档统计 ====="
for f in src/main/resources/kb/*.md src/main/resources/kb/*.txt; do
  if [ -f "$f" ]; then
    lines=$(wc -l < "$f")
    echo "文档：$(basename "$f") —— $lines 行"
  fi
done
echo "===== 统计完成 ====="
```

当 script-runner 技能被激活后，模型执行 `bash scripts/report.sh`，读取输出并整理成中文汇报。

**安全建议**：

- `workingDirectory` 设为最小范围
- 生产环境：叠加命令白名单、超时熔断、审计日志
- 不建议给模型"任意命令执行"权限

---

## 7. 完整组装示例（demo14）

```java
@RestController
@RequestMapping("/api/demo14")
public class Demo14AgentSkillsController {

    private final ChatClient chatClient;

    public Demo14AgentSkillsController(ChatClient.Builder chatClientBuilder) {
        // 1) 技能注册表
        ToolCallback skillsTool = SkillsTool.builder()
                .addSkillsResource(new ClassPathResource("skills/kb-reader/SKILL.md"))
                .addSkillsResource(new ClassPathResource("skills/script-runner/SKILL.md"))
                .build();

        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(skillsTool)                  // 技能工具
                .defaultTools(FileSystemTools.builder()            // 读文件
                        .allowedDirectory(".").build())
                .defaultTools(ShellTools.builder()                 // 跑脚本
                        .workingDirectory(".").build())
                .defaultSystem(s -> s.text(                        // 系统角色引导
                        "你是'小北优选咖啡店'的智能助手。你拥有两项技能：" +
                        "kb-reader 与 script-runner。" +
                        "当任务匹配技能描述时，先按 SKILL.md 的步骤操作，再回答。"))
                .build();
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return this.chatClient.prompt().user(question).call().content();
    }
}
```

---

## 8. 技能 vs 普通工具

| 维度 | 普通 @Tool | Agent Skills |
|------|-----------|--------------|
| 定义方式 | Java 注解 | SKILL.md 文件 |
| 能力范围 | 单一函数 | 读文件 + 跑脚本 + 查 API（复合） |
| 加载方式 | 一次性全量 | 渐进式按需 |
| 可扩展性 | 写在代码里 | 新增 SKILL.md 目录即可 |
| 适用场景 | 查数据、简单动作 | 需要多步骤、读文档、执行脚本的复杂任务 |

---

## 9. SKILL.md 编写规范

### 好的 SKILL.md

```markdown
---
name: kb-reader
description: 阅读门店私有知识库，回答店铺政策类问题。当用户询问会员积分、退货、配送时使用。
---

# 门店知识库阅读

## 步骤
1. 用 Read 工具读取 src/main/resources/kb/shop-policy.md
2. 找到与用户问题最相关的内容
3. 基于文档回答，未提及的内容说"文档中未提及"
```

### 要点

- **description 要精确**：明确说明触发条件，防止误触发
- **步骤要具体**：具体到文件名、工具名，不给模型模糊指令
- **边界要清楚**：明确"什么可以做，什么不可以做"（如"不要编造"）
- **格式统一**：YAML 头 + Markdown 体，保持项目内一致

---

## 下一步

了解 Agent Skills 后，深入 ETL 文档管道：[06 · ETL 文档管道](./06-etl-pipeline-guide.md)。
