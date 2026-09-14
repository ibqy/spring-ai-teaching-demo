package com.xb.springai.controller.demo14;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;

/**
 * demo14：Agent Skills —— 让 Skill 读取文档、执行脚本
 *
 * <p>作者：ibqy | 日期：2026-09-14</p>
 *
 * <p><b>教学知识点</b>：Agent Skills（智能体技能）是把"能力"打包成目录：
 * 每个技能内含 SKILL.md（YAML 头 + Markdown 指令），可附带脚本与参考资料。
 * Spring AI 2.0 通过社区库 spring-ai-agent-utils 提供官方推荐的三件套：</p>
 * <ol>
 *     <li>SkillsTool（必需）：扫描技能注册表，把技能清单暴露给模型，按需加载完整指令</li>
 *     <li>FileSystemTools（读文档）：让模型用 Read 等工具读取项目内文档（本例是 kb 知识库）</li>
 *     <li>ShellTools（执行脚本）：让模型用 Bash 运行项目内脚本，执行边界限定在项目目录</li>
 * </ol>
 *
 * <p><b>渐进式披露（Progressive Disclosure）</b>：启动时只向模型暴露每个技能的
 * name + description（Discovery 发现）；用户提问与某技能语义匹配时才把完整 SKILL.md
 * 指令加载进上下文（Activation 激活）；执行时再按需读文件 / 跑脚本（Execution 执行）。
 * 因此注册几十上百个技能也不会撑爆上下文窗口。</p>
 *
 * <p><b>安全边界</b>：{@code workingDirectory(".")} 让 ShellTools 只能在项目目录内
 * 执行命令，{@code allowedDirectory(".")} 限定 FileSystemTools 的文件访问范围。
 * 生产环境还应叠加命令白名单、超时熔断、审计日志等手段。</p>
 */
@RestController
@RequestMapping("/api/demo14")
public class Demo14AgentSkillsController {

    private final ChatClient chatClient;

    /**
     * 构造时把"技能工具 + 读文档工具 + 脚本工具"一起注册进 ChatClient 的默认调用链。
     */
    public Demo14AgentSkillsController(ChatClient.Builder chatClientBuilder) {
        // 1) 技能注册表：扫描 classpath:skills/ 下内置的两个示例技能（kb-reader、script-runner）
        ToolCallback skillsTool = SkillsTool.builder()
                .addSkillsResource(new ClassPathResource("skills/kb-reader/SKILL.md"))
                .addSkillsResource(new ClassPathResource("skills/script-runner/SKILL.md"))
                .build();

        this.chatClient = chatClientBuilder
                // 2) 技能工具：让模型能"发现 + 加载"技能（必选）
                .defaultToolCallbacks(skillsTool)
                // 3) 读文档工具：限制只能访问项目目录（含 kb/ 知识库、scripts/ 脚本）
                .defaultTools(FileSystemTools.builder()
                        .allowedDirectory(".")
                        .build())
                // 4) 执行脚本工具：命令限制在项目目录内执行（安全边界）
                .defaultTools(ShellTools.builder()
                        .workingDirectory(".")
                        .build())
                // 5) 系统角色：说明自身的能力清单，引导模型正确使用技能
                .defaultSystem(s -> s.text(
                        "你是'小北优选咖啡店'的智能助手。你拥有两项技能：\n" +
                        "kb-reader（阅读门店知识库文档回答政策问题）与 script-runner（执行项目内脚本并汇报）。\n" +
                        "当任务匹配技能描述时，先按 SKILL.md 的步骤操作，再回答；不要编造未读到的内容。"))
                .build();
    }

    /**
     * GET /api/demo14/ask?question=会员积分怎么算？
     * GET /api/demo14/ask?question=运行脚本统计一下知识库
     * 模型会自动判断命中哪个技能，再决定要不要读文档 / 执行脚本。
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "帮我查看会员积分规则") String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
