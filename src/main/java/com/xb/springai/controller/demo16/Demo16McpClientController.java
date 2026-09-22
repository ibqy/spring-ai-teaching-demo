package com.xb.springai.controller.demo16;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo16：MCP 客户端 —— 通过 Model Context Protocol 接入外部工具服务
 *
 * <p>作者：ibqy | 日期：2026-09-15</p>
 *
 * <p><b>教学知识点</b>：MCP（Model Context Protocol）是 2024 年底由 Anthropic 提出的
 * "AI 应用与外部工具之间的 USB-C 标准"，OpenAI / Google / 微软均已采纳。
 * 本 Demo 演示 Spring AI 作为 <b>MCP 客户端</b> 连接一个官方文件系统 MCP Server：
 * 模型遇到读写文件类问题时，会自动调用 MCP 服务器暴露的工具，而不是自己"编造"内容。</p>
 *
 * <p><b>工作原理</b>：</p>
 * <ol>
 *     <li>application.yml 配置了 {@code spring.ai.mcp.client.stdio.servers.filesystem}
 *         —— 启动一个 {@code npx} 子进程（stdio 传输方式）作为 MCP 服务器</li>
 *     <li>Spring AI 的 MCP 自动配置会把该服务器的工具列表拉取下来，
 *         打包成一个 {@link ToolCallbackProvider} Bean（工具名如 Read / Write / ListDirectory）</li>
 *     <li>把该 Provider 挂到 ChatClient 的默认工具链，模型就能按需调用 MCP 工具</li>
 * </ol>
 *
 * <p><b>启动前准备</b>（本 Demo 默认关闭，不影响其他 demo）：</p>
 * <pre>
 * # 1) 安装 Node.js（npx 随附）：https://nodejs.org
 * # 2) 设置环境变量后重启应用：
 * #    export MCP_ENABLED=true      # Linux / macOS
 * #    $env:MCP_ENABLED="true"      # Windows PowerShell
 * # 3) 访问 /api/demo16/status 确认 MCP 工具已就绪
 * </pre>
 *
 * <p><b>演示问题示例</b>：</p>
 * <pre>
 * GET /api/demo16/ask?question=查看当前项目目录下有哪些文件
 * GET /api/demo16/ask?question=读取 src/main/resources/kb/shop-intro.txt 并总结
 * </pre>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo16")
public class Demo16McpClientController {

    private final ChatClient chatClient;

    /** MCP 工具提供者：由 spring-ai-starter-mcp-client 自动配置（未启用时为 null） */
    private final ToolCallbackProvider mcpToolProvider;

    /**
     * 构造时注入 MCP 工具提供者。使用 {@code @Autowired(required = false)}：
     * 未配置/未启用 MCP 服务器时，Provider 为 null，应用照常启动（降级为普通对话）。
     */
    public Demo16McpClientController(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) ToolCallbackProvider mcpToolCallbackProvider) {
        this.mcpToolProvider = mcpToolCallbackProvider;

        if (mcpToolCallbackProvider != null) {
            this.chatClient = chatClientBuilder
                    // 1) 把 MCP 服务器的全部工具挂到默认工具链（模型按需调用）
                    .defaultToolCallbacks(mcpToolCallbackProvider)
                    // 2) 系统角色引导：明确告知模型可调用 MCP 工具
                    .defaultSystem(s -> s.text(
                            "你是一个接入了 MCP（Model Context Protocol）工具服务的智能助手。\n" +
                            "工具列表中的 Read / Write / ListDirectory 等能力来自文件系统 MCP 服务器。\n" +
                            "当用户询问文件内容、目录结构或需要读写文件时，优先调用这些 MCP 工具完成，\n" +
                            "并将工具返回的真实结果整理成中文回答；不要编造未读取到的内容。"))
                    .build();
        } else {
            // 降级模式：未启用 MCP 时的普通对话（提示用户如何开启）
            this.chatClient = chatClientBuilder
                    .defaultSystem("当前未接入 MCP 服务器（MCP_ENABLED=false）。" +
                            "请阅读 docs/10-mcp-guide.md 了解如何启用 demo16。")
                    .build();
        }
    }

    /**
     * GET /api/demo16/ask?question=查看当前项目目录下有哪些文件
     * 模型会自动决定是否调用 MCP 工具，再基于工具返回结果组织回答。
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "查看当前项目目录下有哪些文件") String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * GET /api/demo16/status —— 查看 MCP 连接状态与可用工具清单。
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mcpEnabled", mcpToolProvider != null);

        if (mcpToolProvider != null) {
            var toolCallbacks = mcpToolProvider.getToolCallbacks();
            result.put("toolCount", toolCallbacks.length);
            result.put("tools", java.util.Arrays.stream(toolCallbacks)
                    .map(tc -> Map.of(
                            "name", tc.getToolDefinition().name(),
                            "description", tc.getToolDefinition().description()))
                    .toList());
        } else {
            result.put("tools", "未启用（MCP_ENABLED=false）");
            result.put("hint", "设置环境变量 MCP_ENABLED=true 并重启，即可连接文件系统 MCP Server");
        }
        return result;
    }
}
