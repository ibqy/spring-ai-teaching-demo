package com.xb.springai.controller.demo19;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo19：A2A（Agent-to-Agent）—— 智能体间通信协议
 *
 * <p>作者：ibqy | 日期：2026-09-22</p>
 *
 * <p><b>教学知识点</b>：A2A（Agent-to-Agent Protocol）是 Google 2025 年提出的
 * 智能体间通信标准，类似于 MCP 解决"AI 与工具"的连接问题，A2A 解决的是
 * "Agent 与 Agent"之间的互发现、互调用问题。不同框架（Spring AI / LangChain /
 * AgentScope 等）构建的 Agent 只要遵循 A2A 协议，就能互相协作。</p>
 *
 * <p><b>核心概念</b>：</p>
 * <ul>
 *     <li><b>AgentCard</b>：Agent 的"名片"，包含名称、描述、技能列表、端点地址</li>
 *     <li><b>AgentExecutor</b>：接收远程请求并交给 ChatClient 处理</li>
 *     <li><b>JSON-RPC</b>：A2A 使用 JSON-RPC 2.0 作为通信格式</li>
 * </ul>
 *
 * <p><b>工作原理</b>：</p>
 * <ol>
 *     <li>应用启动时，spring-ai-a2a-server-autoconfigure 自动注册 AgentCard 和 AgentExecutor</li>
 *     <li>对外暴露 {@code GET /a2a/card}（AgentCard 发现）和 {@code POST /a2a/}（JSON-RPC 消息处理）</li>
 *     <li>其他 Agent 通过 AgentCard 发现本 Agent 的能力，然后通过 JSON-RPC 发送任务</li>
 * </ol>
 *
 * <p><b>启动前准备</b>（本 Demo 默认关闭，不影响其他 demo）：</p>
 * <pre>
 * # 设置环境变量启用 A2A 服务端：
 * #    export A2A_ENABLED=true      # Linux / macOS
 * #    $env:A2A_ENABLED="true"      # Windows PowerShell
 * # 访问 /api/demo19/status 查看 AgentCard 信息
 * # 访问 /a2a/card 查看 A2A 协议标准名片
 * </pre>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo19")
public class Demo19A2aController {

    private final ChatClient chatClient;
    private final boolean a2aEnabled;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * A2A 是否启用通过环境变量控制。
     * 无论是否启用，本控制器都提供本地对话测试能力。
     */
    public Demo19A2aController(
            ChatClient.Builder chatClientBuilder,
            @Value("${spring.ai.a2a.server.enabled:false}") boolean a2aEnabled) {
        this.a2aEnabled = a2aEnabled;
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个支持 A2A（Agent-to-Agent）协议的智能助手。\n"
                        + "你可以被其他 Agent 通过 A2A 协议发现和调用。\n"
                        + "请用中文回答用户的问题，简洁专业。")
                .build();
    }

    /**
     * GET /api/demo19/ask?message=你好
     * 本地测试接口：模拟一个 Agent 向本 Agent 发送消息。
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "你好，请介绍一下你的能力") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * GET /api/demo19/status —— 查看 A2A 服务端状态与 AgentCard 信息。
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("a2aEnabled", a2aEnabled);
        result.put("agentName", "spring-ai-teaching-demo-agent");
        result.put("agentDescription", "Spring AI 教学演示 Agent —— 支持通用对话与知识问答");
        result.put("agentCardUrl", "http://localhost:" + serverPort + "/a2a/card");
        result.put("jsonRpcUrl", "http://localhost:" + serverPort + "/a2a/");

        if (a2aEnabled) {
            result.put("skills", new String[]{
                    "通用对话：回答各类中文问题",
                    "知识问答：基于内置知识库回答业务问题"
            });
            result.put("hint", "A2A 服务端已启用，其他 Agent 可通过 /a2a/card 发现本 Agent");
        } else {
            result.put("hint", "A2A 服务端未启用（A2A_ENABLED=false）。"
                    + "设置环境变量 A2A_ENABLED=true 并重启即可暴露标准 A2A 端点。");
        }
        return result;
    }
}
