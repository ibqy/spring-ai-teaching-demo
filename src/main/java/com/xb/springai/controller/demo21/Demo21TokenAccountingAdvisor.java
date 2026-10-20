package com.xb.springai.controller.demo21;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * demo21：Token 用量计量 Advisor —— 生产环境的「电表」
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：大模型按 Token 计费，看不见用量就等于看不见成本。
 * 本 Demo 在 demo12（自定义 Advisor）基础上再进一步：从「打印日志」升级为
 * 「<b>聚合统计</b>」——把每次模型调用的 Token 用量累计起来，随时可查。</p>
 *
 * <p><b>实现要点</b>：</p>
 * <ol>
 *   <li>实现 {@link CallAdvisor}（与 demo12 同一扩展点），在 {@code chain.nextCall(request)}
 *       之后从 {@link ChatResponse#getMetadata()} 拿到 {@link Usage} 并记账</li>
 *   <li>计数器用 {@link LongAdder}：Advisor 是单例 Bean，并发请求同时写，
 *       LongAdder 比 synchronized / AtomicInteger 更适合「多写少读」的计数场景</li>
 *   <li>健壮性：部分模型/代理不返回 usage（或返回 null），必须容错，
 *       不能因为统计失败影响主链路</li>
 *   <li>成本估算：单价通过构造函数注入（按所用模型真实单价填写），
 *       Advisor 只做算术，不关心价格从哪来</li>
 * </ol>
 *
 * <p><b>生产提示</b>：真实系统应把用量写入 Redis / Prometheus / 计费库，
 * 并按 conversationId / userId / model 维度拆分统计；Advisor 里可以拿到
 * {@link ChatClientRequest} 的用户消息与上下文，按需扩展维度即可。</p>
 *
 * @author ibqy
 */
public class Demo21TokenAccountingAdvisor implements CallAdvisor {

    /** 输入单价（元 / 百万 Token），按实际模型填写；0 表示不估算成本 */
    private final double promptPricePerMillion;

    /** 输出单价（元 / 百万 Token），按实际模型填写；0 表示不估算成本 */
    private final double completionPricePerMillion;

    /** 累计调用次数（含未返回用量的调用） */
    private final LongAdder callCount = new LongAdder();

    /** 累计成功拿到用量的调用次数 */
    private final LongAdder meteredCallCount = new LongAdder();

    /** 累计输入 Token 数 */
    private final LongAdder promptTokens = new LongAdder();

    /** 累计输出 Token 数 */
    private final LongAdder completionTokens = new LongAdder();

    /**
     * @param promptPricePerMillion     输入单价（元 / 百万 Token），0 = 不算钱
     * @param completionPricePerMillion 输出单价（元 / 百万 Token），0 = 不算钱
     */
    public Demo21TokenAccountingAdvisor(double promptPricePerMillion, double completionPricePerMillion) {
        this.promptPricePerMillion = promptPricePerMillion;
        this.completionPricePerMillion = completionPricePerMillion;
    }

    /**
     * Advisor 名称（用于日志/调试标识）。
     */
    @Override
    public String getName() {
        return "tokenAccountingAdvisor";
    }

    /**
     * 执行顺序：数字越小越靠外层。这里放在较靠前的位置（0），
     * 确保它能包住后续所有 Advisor，统计到的是「最终」用量。
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 在模型调用「后」记账：从响应元数据中取出本次用量并累计。
     *
     * @param request 本次大模型请求（含用户消息等）
     * @param chain   调用链，必须调用 chain.nextCall(request) 才能继续走到模型
     * @return 模型的最终响应（原样返回，不改变业务语义）
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1) 先放行，让调用链继续走到模型（Advisor 是「包一层」，不是「拦一层」）
        ChatClientResponse response = chain.nextCall(request);

        // 2) 调用后记账：只读响应，不修改，绝不影响业务返回
        if (response != null && response.chatResponse() != null) {
            record(response.chatResponse());
        }
        return response;
    }

    /**
     * 把一次模型响应的 Token 用量累计到计数器。
     *
     * <p>包级可见（package-private）是为了让单元测试能直接构造
     * {@link ChatResponse} 验证记账逻辑，无需启动 Spring 容器、无需真实调用大模型。</p>
     *
     * @param chatResponse 模型响应（可能没有 usage 元数据，必须容错）
     */
    void record(ChatResponse chatResponse) {
        // 每次调用都计数（无论是否拿到用量）——调用量本身也是成本信号
        callCount.increment();

        // 容错：getMetadata() 可能为 null；getUsage() 也可能为 null（部分模型不返回）
        if (chatResponse.getMetadata() == null) {
            return;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage == null) {
            return;
        }

        meteredCallCount.increment();
        // 各字段同样可能为 null，逐个判空累加
        if (usage.getPromptTokens() != null) {
            promptTokens.add(usage.getPromptTokens());
        }
        if (usage.getCompletionTokens() != null) {
            completionTokens.add(usage.getCompletionTokens());
        }
    }

    /**
     * 生成当前用量快照（供 /api/demo21/usage 查询）。
     *
     * @return 不可变的统计 Map：调用次数、输入/输出 Token、合计、估算成本等
     */
    public Map<String, Object> snapshot() {
        long calls = callCount.sum();
        long metered = meteredCallCount.sum();
        long prompt = promptTokens.sum();
        long completion = completionTokens.sum();
        long total = prompt + completion;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("callCount", calls);
        result.put("meteredCallCount", metered);
        result.put("promptTokens", prompt);
        result.put("completionTokens", completion);
        result.put("totalTokens", total);
        // 单次平均 Token（保留整数，避免浮点噪音）
        result.put("avgTokensPerMeteredCall", metered == 0 ? 0 : total / metered);
        // 估算成本（元）。单价为 0 时返回 0，表示「未配置单价，不估算」
        double cost = prompt / 1_000_000.0 * promptPricePerMillion
                + completion / 1_000_000.0 * completionPricePerMillion;
        result.put("estimatedCostYuan", Math.round(cost * 1_000_000) / 1_000_000.0);
        result.put("priceNote", promptPricePerMillion == 0 && completionPricePerMillion == 0
                ? "未配置模型单价（application.yml 的 demo21.*.price-per-million），成本按 0 展示"
                : "按配置单价估算：输入 " + promptPricePerMillion + " 元/百万 Token，输出 "
                        + completionPricePerMillion + " 元/百万 Token");
        return result;
    }

    /**
     * 清零所有计数器（供 /api/demo21/usage/reset 调用）。
     */
    public void reset() {
        callCount.reset();
        meteredCallCount.reset();
        promptTokens.reset();
        completionTokens.reset();
    }
}
