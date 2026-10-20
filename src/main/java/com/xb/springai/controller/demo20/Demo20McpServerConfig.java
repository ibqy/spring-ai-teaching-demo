package com.xb.springai.controller.demo20;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * demo20：MCP 服务端工具发布配置
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：这是整个 demo20 的"关键一跳"——把工具 Bean 声明成
 * {@link ToolCallbackProvider} Bean。引入 {@code spring-ai-starter-mcp-server-webmvc}
 * 之后，Spring AI 的 MCP 服务端自动配置
 * （{@code ToolCallbackConverterAutoConfiguration}）会自动收集容器内所有
 * {@code ToolCallbackProvider} Bean，转换成 MCP 协议的工具规格并发布到
 * {@code /mcp} 端点。<b>业务代码到这里就结束了，剩下的协议工作全部由自动配置完成。</b></p>
 *
 * <p><b>为什么需要这个类</b>：demo09 是在控制器构造器里"就地"创建 Provider 供
 * ChatClient 使用；而 MCP 服务端要发布的是<b>容器级 Bean</b>，必须用 {@code @Bean}
 * 声明才能被自动配置发现。对比着看，能更好理解"Provider 用在哪"的两种姿势。</p>
 *
 * <p><b>注意</b>：容器内<b>所有</b> {@code ToolCallbackProvider} Bean 都会被发布到
 * MCP 端点。如果哪天你把 demo09 的 Provider 也提成 Bean，它的工具会一并对外暴露——
 * 生产环境请通过独立部署或网络边界控制暴露面。</p>
 *
 * @author ibqy
 */
@Configuration
public class Demo20McpServerConfig {

    /**
     * 把 {@link Demo20ShopTools} 打包成 MCP 服务端可发布的工具提供者。
     *
     * <p>{@link MethodToolCallbackProvider#builder()} 会扫描 Bean 内所有
     * {@code @Tool} 方法（本例三个：queryOrder / queryLogistics / getRefundPolicy），
     * 为每个方法生成包含 name / description / 输入 JSON Schema 的工具定义——
     * 这些定义就是 MCP 客户端通过 {@code tools/list} 能看到的内容。</p>
     *
     * @param shopTools 由 Spring 注入的工具 Bean
     * @return 统一的工具提供者（同时被 MCP 服务端自动配置收集发布）
     */
    @Bean
    public ToolCallbackProvider demo20ShopToolProvider(Demo20ShopTools shopTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(shopTools)
                .build();
    }
}
