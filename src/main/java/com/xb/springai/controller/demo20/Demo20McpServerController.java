package com.xb.springai.controller.demo20;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * demo20：MCP 服务端 —— 把自己的工具发布成 MCP 服务
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：MCP（Model Context Protocol）有两个角色：</p>
 * <ul>
 *   <li><b>Host / Client</b>（demo16）：AI 应用主动连接外部 MCP 服务器，消费工具</li>
 *   <li><b>Server</b>（本 demo20）：把自己的工具按 MCP 协议发布出去，供任何客户端调用</li>
 * </ul>
 *
 * <p>引入 {@code spring-ai-starter-mcp-server-webmvc} 后，本应用自动成为 MCP 服务器：</p>
 * <ol>
 *   <li>{@link Demo20McpServerConfig} 声明的 {@link ToolCallbackProvider} Bean
 *       被自动配置收集，转换成 MCP 工具规格</li>
 *   <li>应用对外暴露 MCP 端点（默认 {@code POST /mcp}，streamable HTTP 传输）</li>
 *   <li>客户端完成 {@code initialize} 握手后，用 {@code tools/list} 就能看到
 *       queryOrder / queryLogistics / getRefundPolicy 三个工具，随后用
 *       {@code tools/call} 远程调用——和调用本地方法一样</li>
 * </ol>
 *
 * <p><b>为什么选 webmvc 变体</b>：本项目是 Servlet（Tomcat）栈
 * （{@code spring-boot-starter-web}），对应 {@code -server-webmvc} starter；
 * 响应式 WebFlux 栈才用 {@code -server-webflux}。两者发布的工具完全相同，
 * 只是传输层实现不同。</p>
 *
 * <p><b>与 demo16 的关系</b>：demo16（客户端）默认关闭（要 Node.js 起 npx 子进程）；
 * demo20（服务端）零外部依赖，启动即生效。两者可在同一应用内共存——
 * 一个应用完全可以同时是「别人的客户端」和「别人的服务器」。</p>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo20")
public class Demo20McpServerController {

    /**
     * MCP 端点路径。默认值与 Spring AI 的 MCP 服务端自动配置保持一致
     * （{@code spring.ai.mcp.server.streamable-http.mcp-endpoint}，默认 /mcp），
     * 这里通过 @Value 读取，避免应用代码直接依赖自动配置的内部属性类。
     */
    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}")
    private String mcpEndpoint;

    /** MCP 传输协议（streamable / sse / stateless），默认 streamable */
    @Value("${spring.ai.mcp.server.protocol:streamable}")
    private String protocol;

    /** MCP 服务器名称，客户端握手时能看到 */
    @Value("${spring.ai.mcp.server.name:spring-ai-teaching-demo}")
    private String serverName;

    /** MCP 服务器版本号 */
    @Value("${spring.ai.mcp.server.version:1.0.0}")
    private String serverVersion;

    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * 注入 demo20 配置的 Provider，用于在 status 端点展示「已发布的工具清单」。
     * 注意：这里注入只是为了演示查看；MCP 端点真正发布工具靠的是自动配置收集，
     * 控制器不参与协议处理。
     */
    public Demo20McpServerController(ToolCallbackProvider demo20ShopToolProvider) {
        this.toolCallbackProvider = demo20ShopToolProvider;
    }

    /**
     * GET /api/demo20/status —— 查看 MCP 服务器状态与已发布的工具清单。
     *
     * <p>浏览器直接访问即可。看到的 tools 数组，就是 MCP 客户端执行
     * {@code tools/list} 时会收到的内容。</p>
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverName", serverName);
        result.put("serverVersion", serverVersion);
        result.put("protocol", protocol);
        result.put("mcpEndpoint", mcpEndpoint);
        result.put("transport", "STREAMABLE_HTTP（streamable HTTP，客户端 POST JSON 到 " + mcpEndpoint + "）");

        var callbacks = toolCallbackProvider.getToolCallbacks();
        result.put("toolCount", callbacks.length);
        List<Map<String, String>> tools = Arrays.stream(callbacks)
                .map(tc -> {
                    Map<String, String> tool = new LinkedHashMap<>();
                    tool.put("name", tc.getToolDefinition().name());
                    tool.put("description", tc.getToolDefinition().description());
                    return tool;
                })
                .toList();
        result.put("tools", tools);
        return result;
    }

    /**
     * GET /api/demo20/inspector —— 获取 MCP Inspector 连接配置与操作指引。
     *
     * <p>MCP Inspector 是官方调试工具（{@code npx @modelcontextprotocol/inspector}），
     * 可以把下面的配置粘进去，可视化地点 List Tools / Call Tool。</p>
     */
    @GetMapping("/inspector")
    public Map<String, Object> inspector() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 1) MCP Inspector 的连接配置（Transport 选 Streamable HTTP）
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("transport", "Streamable HTTP");
        config.put("url", "http://localhost:8080" + mcpEndpoint);
        result.put("inspectorConfig", config);

        // 2) 手工 curl 验证四步曲（streamable HTTP 是有会话状态的协议，四步顺序不能乱）：
        //    ① initialize 握手 —— 响应头里带回 mcp-session-id（后续请求必须带它）
        //    ② notifications/initialized 通知（无 id，服务器不回包）
        //    ③ tools/list 列出工具（响应是 SSE 事件流，看 data: 行）
        //    ④ tools/call 调用工具
        //    说明：第 ① 步响应头的 mcp-session-id 每次握手都不同，curl 示例里的
        //    <SESSION_ID> 需要替换成你自己那次握手拿到的值。
        result.put("curlSteps", List.of(
                // 第一步：initialize 握手，协商协议版本（-i 才能看到响应头里的 mcp-session-id）
                "curl -i -X POST http://localhost:8080" + mcpEndpoint + " -H \"Content-Type: application/json\" "
                        + "-H \"Accept: application/json, text/event-stream\" "
                        + "-d '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},"
                        + "\"clientInfo\":{\"name\":\"curl\",\"version\":\"1.0\"}}}'   "
                        + "# 从响应头取出 mcp-session-id，后面三步都要带上",
                // 第二步：发送 initialized 通知（notification 无 id，服务器不回包）
                "curl -X POST http://localhost:8080" + mcpEndpoint + " -H \"Content-Type: application/json\" "
                        + "-H \"mcp-session-id: <SESSION_ID>\" "
                        + "-d '{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}'",
                // 第三步：列出工具（SSE 响应，取 data: 行即为 JSON）
                "curl -X POST http://localhost:8080" + mcpEndpoint + " -H \"Content-Type: application/json\" "
                        + "-H \"Accept: application/json, text/event-stream\" "
                        + "-H \"mcp-session-id: <SESSION_ID>\" "
                        + "-d '{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}'",
                // 第四步：调用工具（返回 isError=false 与工具执行结果）
                "curl -X POST http://localhost:8080" + mcpEndpoint + " -H \"Content-Type: application/json\" "
                        + "-H \"Accept: application/json, text/event-stream\" "
                        + "-H \"mcp-session-id: <SESSION_ID>\" "
                        + "-d '{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"queryOrder\",\"arguments\":{\"orderId\":\"A1001\"}}}'"));

        // 3) 用 Spring AI 客户端连接「自己」的演练思路（进阶）
        result.put("selfConnectHint", "本应用同时也是 MCP 客户端（demo16）：在 application.yml 的 "
                + "spring.ai.mcp.client.stdio.servers 旁新增一个指向 http://localhost:8080" + mcpEndpoint
                + " 的客户端配置，即可让模型调用本应用自己发布的工具，"
                + "完整体验「服务器 → 客户端 → 模型」的闭环。详见 docs/14-mcp-server-guide.md");
        return result;
    }
}
