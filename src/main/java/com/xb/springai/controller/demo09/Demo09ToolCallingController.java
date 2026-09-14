package com.xb.springai.controller.demo09;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo09：工具自动注册（ToolCallbackProvider）—— 从需求到实现进阶
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>进阶点（相较 demo05）</b>：
 * <ol>
 *   <li>demo05 是"把单个工具对象临时传给本次调用"；demo09 是你的工具越来越多，
 *       需要一个统一的管理方式——用 {@link ToolCallbackProvider} 集中注册。</li>
 *   <li>用 {@link MethodToolCallbackProvider#builder()} 把多个 Spring Bean
 *       （内含 @Tool 方法）打包成一个 Provider，一次注册、全局可用。</li>
 *   <li>好处：工具与控制器解耦，新增工具只需新增 Bean，无需改动控制器注册逻辑。</li>
 * </ol></p>
 */
@RestController
@RequestMapping("/api/demo09")
public class Demo09ToolCallingController {

    private final ChatClient chatClient;
    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * 把所有工具 Bean 打包成一个 Provider。
     * 注入 OrderTools（内含 queryOrder / getRefundPolicy 两个 @Tool 方法），
     * 通过 MethodToolCallbackProvider 生成统一的 ToolCallbackProvider。
     */
    public Demo09ToolCallingController(ChatClient.Builder chatClientBuilder, OrderTools orderTools) {
        // 方式一：把 Bean 转成 Provider，集中管理
        this.toolCallbackProvider = MethodToolCallbackProvider.builder()
                .toolObjects(orderTools)
                .build();

        // 方式二：直接把 Provider 注册进 ChatClient（作为默认工具），
        //         之后每次调用都会自动带上这批工具，无需再手动 .tools(...)
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(this.toolCallbackProvider)
                .build();
    }

    /**
     * GET /api/demo09/orders?question=帮我查一下订单A1001到哪了
     * 模型会自动调用 queryOrder("A1001")。
     */
    @GetMapping("/orders")
    public String orders(@RequestParam(defaultValue = "帮我查一下订单 A1001 到哪了") String question) {
        // 无需再手动 .tools(...)，因为工具已在构建 ChatClient 时默认注入
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * GET /api/demo09/refund?question=我买的咖啡能退吗
     * 模型会自动调用 getRefundPolicy() 返回退换货规则。
     */
    @GetMapping("/refund")
    public String refund(@RequestParam(defaultValue = "我买的咖啡豆能退款吗？") String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}