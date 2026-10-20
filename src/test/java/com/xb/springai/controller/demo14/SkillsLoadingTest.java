package com.xb.springai.controller.demo14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;
import org.springaicommunity.agent.tools.SkillsTool;

/**
 * Agent Skills 加载方式回归测试（离线可跑，不依赖大模型）。
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>背景</b>：spring-ai-agent-utils 从 0.12.0 升级到 0.13.0 后，
 * {@code addSkillsResource(...)} 的语义从「直接读 SKILL.md 文件」变为
 * 「扫描技能目录」。本项目升级后从未启动验证，导致应用启动即报
 * {@code At least one skill must be configured}（demo14/demo15 全部不可用）。
 * 本测试把两种正确的加载方式与一个典型错误用法固定下来，防止再回归。</p>
 */
@DisplayName("SkillsTool 技能加载回归测试")
class SkillsLoadingTest {

    /** 标准的 SKILL.md 内容（YAML frontmatter + Markdown 指令） */
    private static final String SKILL_MD = """
            ---
            name: probe-skill
            description: 探针技能：用于验证 SkillsTool 的目录加载机制
            ---

            # 探针技能

            1. 输出 hello
            """;

    @Test
    @DisplayName("classpath 技能目录可以正常加载（demo14/demo15 降级模式）")
    void shouldLoadSkillFromClasspathDirectory() {
        // 0.13.0 正确姿势：传「技能目录」（目录内放 SKILL.md）
        ToolCallback tool = SkillsTool.builder()
                .addSkillsResource(new ClassPathResource("skills/kb-reader"))
                .build();

        // SkillsTool 的工具描述里会列出所有已注册技能的 name + description
        String description = tool.getToolDefinition().description();
        assertThat(description).contains("kb-reader");
    }

    @Test
    @DisplayName("文件系统技能目录可以正常加载（demo15 Nacos 落地模式）")
    void shouldLoadSkillFromFileSystemDirectory(@TempDir Path tempDir) throws IOException {
        // 模拟 demo15 的 Nacos 落地结构：<root>/<技能名>/SKILL.md
        Path skillDir = Files.createDirectories(tempDir.resolve("probe-skill"));
        Files.writeString(skillDir.resolve("SKILL.md"), SKILL_MD, StandardCharsets.UTF_8);

        ToolCallback tool = SkillsTool.builder()
                .addSkillsDirectory(tempDir.toString())
                .build();

        assertThat(tool.getToolDefinition().description()).contains("probe-skill");
    }

    @Test
    @DisplayName("直接传 SKILL.md 文件路径会失败——记录 0.13.0 的 API 陷阱")
    void shouldFailWhenPassingSkillMdFileDirectly() {
        // 错误用法（0.12 时代的写法）：把 SKILL.md 文件本身传给 addSkillsResource。
        // 0.13.0 按目录扫描，扫不到任何技能，build() 时报错。
        assertThatThrownBy(() -> SkillsTool.builder()
                .addSkillsResource(new ClassPathResource("skills/kb-reader/SKILL.md"))
                .build())
                .hasMessageContaining("At least one skill must be configured");
    }
}
