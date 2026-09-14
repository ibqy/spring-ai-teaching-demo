package com.xb.springai.controller.demo12;

import java.lang.System.Logger;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * demo12：自定义日志顾问 —— 深入 Spring AI 的扩展点（Advisor）
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：Advisor 是 Spring AI 最强大的扩展机制，它像 AOP 切面一样
 * 可以在每次"调用大模型"前后插入自定义逻辑。定义一个 Advisor 只要实现
 * {@link CallAdvisor} 接口，在 {@link #adviseCall} 里"包一层"：</p>
 * <ol>
 *   <li>调用 {@code chain.nextCall(request)} <em>之前</em>：可以记录请求、做缓存、限流、鉴权</li>
 *   <li>调用之后拿到 {@link ChatClientResponse}：可以统计耗时、记录 Token 用量、审计</li>
 * </ol>
 *
 * <p>这个 Demo 实现一个记录"请求内容 + 响应 Token"的日志 Advisor。你可以把它改造成
 * 统计查询成本、拦截敏感词、熔断限流等企业级能力——原理完全一致。</p>
 */
@Component
public class Demo12LoggingAdvisor implements CallAdvisor {

    private static final Logger log = System.getLogger(Demo12LoggingAdvisor.class.getName());

    /**
     * 顾问的名称（Advisor 接口要求实现，用于日志/调试标识）。
     */
    @Override
    public String getName() {
        return "loggingAdvisor";
    }

    /**
     * 执行顺序（Advisor 接口继承自 Ordered）。数字越小优先级越高，默认放最后。
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * 真正的方法：在模型调用前后插入逻辑。
     * @param request  本次大模型请求（含用户消息等）
     * @param chain    调用链，必须调用 chain.nextCall(request) 才能继续走到模型
     * @return 模型的最终响应
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // -- 调用前：记录请求内容 --
        log.log(Logger.Level.INFO, "[advisor] 收到请求: {0}", request.prompt().getContents());

        long start = System.currentTimeMillis();
        // -- 真正调用模型（必须继续走完调用链，否则模型不会被调用）--
        ChatClientResponse response = chain.nextCall(request);

        // -- 调用后：统计耗时 --
        long cost = System.currentTimeMillis() - start;
        String output = response.chatResponse() != null ? response.chatResponse().toString() : "";
        log.log(Logger.Level.INFO, "[advisor] 调用完成，耗时 {0} ms => {1}",
                cost, truncate(output));
        return response;
    }

    /** 简单截断超长文本，避免日志刷屏 */
    private String truncate(String s) {
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }
}