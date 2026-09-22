package com.xb.springai.controller.demo09;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * demo09：多工具自动注册 —— 订单工具 Bean
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：真实的 AI 助手往往不止一个工具。我们把"订单查询"和"退款规则"
 * 放在同一个/多个 @Component Bean 里，再用 {@link org.springframework.ai.tool.method.MethodToolCallbackProvider}
 * 把这些 Bean 统一收集成 ToolCallbackProvider，一次性注册给 ChatClient。
 * <ul>
 *   <li>工具逻辑抽离为 Spring Bean，可复用、可注入其他依赖</li>
 *   <li>新增一个 @Tool 方法即自动成为一个可被模型调用的工具</li>
 *   <li>用 Provider 集中管理，比散落在控制器里更清晰</li>
 * </ul></p>
 *
 * @author ibqy
 */
@Component
public class OrderTools {

    /** 模拟订单数据源：订单号 -> 状态与金额 */
    private static final java.util.Map<String, String> ORDERS = java.util.Map.of(
            "A1001", "已发货，金额 299 元",
            "A1002", "待付款，金额 88 元",
            "B3003", "已退款，金额 129 元"
    );

    /**
     * 查询订单当前状态。
     * @param orderId 订单号，形如 A1001
     * @return 订单状态描述
     */
    @Tool(description = "根据订单号查询订单的最新状态，用于回答订单到哪里了、订单状态等问题")
    public String queryOrder(String orderId) {
        return ORDERS.getOrDefault(orderId, "未找到订单 " + orderId + "，请核对订单号。");
    }

    /**
     * 查询标准退换货规则（业务知识）。
     * @return 退换货政策文案
     */
    @Tool(description = "查询小店标准退换货规则，当用户问到能否退货、退款政策时调用")
    public String getRefundPolicy() {
        return "7 天内支持无理由退换（商品完好、不影响二次销售）；生鲜与定制商品除外。";
    }
}