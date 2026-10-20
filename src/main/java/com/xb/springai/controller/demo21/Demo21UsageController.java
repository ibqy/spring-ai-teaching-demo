package com.xb.springai.controller.demo21;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo21：Token 用量与成本可观测 —— 给 AI 应用装上「电表」
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：把 {@link Demo21TokenAccountingAdvisor} 注册为
 * ChatClient 的默认 Advisor，之后<b>每一次</b>模型调用都会自动经过它记账，
 * 业务代码零改动——这就是横切关注点（cross-cutting concern）用 AOP 式
 * 扩展点统一收口的价值（对比 demo12 的日志 Advisor，这里是「可查询的聚合统计」）。</p>
 *
 * <p><b>使用方式</b>：</p>
 * <ol>
 *   <li>先调几次 {@code /api/demo21/chat}（产生用量）</li>
 *   <li>再访问 {@code /api/demo21/usage} 查看累计用量与估算成本</li>
 *   <li>{@code /api/demo21/usage/reset} 清零重新计量</li>
 * </ol>
 *
 * <p><b>模型单价</b>：在 application.yml 配置
 * {@code demo21.prompt-price-per-million} / {@code demo21.completion-price-per-million}
 * （单位：元/百万 Token）。默认 0 = 只统计 Token 不估算金额；
 * 按所用模型真实单价填写后即可看到成本。</p>
 *
 * @author ibqy
 */
@RestController
@RequestMapping("/api/demo21")
public class Demo21UsageController {

    private final ChatClient chatClient;

    /** 计量 Advisor：单例，跨请求累计 */
    private final Demo21TokenAccountingAdvisor accountingAdvisor;

    /**
     * 构造时创建计量 Advisor 并注册为 ChatClient 默认 Advisor。
     *
     * @param chatClientBuilder       Spring AI 自动配置的 ChatClient 构建器
     * @param promptPricePerMillion   输入单价（元/百万 Token），来自 application.yml
     * @param completionPricePerMillion 输出单价（元/百万 Token），来自 application.yml
     */
    public Demo21UsageController(
            ChatClient.Builder chatClientBuilder,
            @Value("${demo21.prompt-price-per-million:0}") double promptPricePerMillion,
            @Value("${demo21.completion-price-per-million:0}") double completionPricePerMillion) {
        this.accountingAdvisor = new Demo21TokenAccountingAdvisor(promptPricePerMillion, completionPricePerMillion);
        this.chatClient = chatClientBuilder
                // 关键一行：把计量 Advisor 挂到默认调用链，之后所有调用自动记账
                .defaultAdvisors(this.accountingAdvisor)
                .build();
    }

    /**
     * GET /api/demo21/chat?message=你好 —— 普通对话（调用会被自动计量）。
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "用一句话介绍 Spring AI") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * GET /api/demo21/usage —— 查看累计 Token 用量与估算成本。
     */
    @GetMapping("/usage")
    public Map<String, Object> usage() {
        return accountingAdvisor.snapshot();
    }

    /**
     * GET /api/demo21/usage/reset —— 清零计数器，返回清零后的快照。
     */
    @GetMapping("/usage/reset")
    public Map<String, Object> reset() {
        accountingAdvisor.reset();
        Map<String, Object> result = new LinkedHashMap<>(accountingAdvisor.snapshot());
        result.put("message", "计数器已清零");
        return result;
    }
}
