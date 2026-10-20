package com.xb.springai.controller.demo20;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo20：MCP 服务端工具 —— 把自己项目的工具发布成 MCP 服务
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：demo16 演示的是"作为 MCP <b>客户端</b>消费别人发布的工具"；
 * 本 Demo 反过来——把自己项目的工具发布成 MCP <b>服务端</b>，让任何 MCP 客户端
 * （Claude Desktop、MCP Inspector、其他 Spring AI 应用）都能通过标准协议发现并调用。</p>
 *
 * <p><b>发布原理</b>（零协议编码）：</p>
 * <ol>
 *   <li>工具方法仍用 Spring AI 的 {@code @Tool} 注解声明（与 demo05/demo09 完全相同的写法）</li>
 *   <li>用 {@link org.springframework.ai.tool.method.MethodToolCallbackProvider} 打包成
 *       {@code ToolCallbackProvider} Bean（见 {@link Demo20McpServerConfig}）</li>
 *   <li>引入 {@code spring-ai-starter-mcp-server-webmvc} 后，Spring AI 的 MCP 服务端
 *       自动配置会收集容器内所有 {@code ToolCallbackProvider} Bean，
 *       自动转换成 MCP 协议的工具规格（tool specifications）并发布到 /mcp 端点</li>
 * </ol>
 *
 * <p><b>关键认知</b>：MCP 不是新框架，而是"工具描述 + 调用"的标准化序列化。
 * 业务代码（本类）与协议细节（自动配置）完全解耦——这正是 MCP 被称为
 * "AI 应用的 USB-C"的原因：插上就能用，不挑客户端。</p>
 *
 * @author ibqy
 */
@Component
public class Demo20ShopTools {

    /** 模拟订单数据源：订单号 -> 状态与金额（教学演示用假数据） */
    private static final Map<String, String> ORDERS = new LinkedHashMap<>();

    static {
        ORDERS.put("A1001", "已发货，金额 299 元");
        ORDERS.put("A1002", "待付款，金额 88 元");
        ORDERS.put("B3003", "已退款，金额 129 元");
    }

    /** 模拟物流数据源：订单号 -> 物流轨迹 */
    private static final Map<String, String> LOGISTICS = Map.of(
            "A1001", "已到达【杭州转运中心】，预计明日 18:00 前送达",
            "A1002", "尚未发货，仓库正在备货"
    );

    /**
     * 查询订单当前状态。
     *
     * @param orderId 订单号，形如 A1001
     * @return 订单状态描述；订单不存在时返回提示文案（而不是抛异常，
     *         让模型能基于结果向用户解释，避免工具报错中断整个对话）
     */
    @Tool(description = "根据订单号查询订单的最新状态，用于回答订单到哪里了、订单状态等问题")
    public String queryOrder(String orderId) {
        return ORDERS.getOrDefault(orderId, "未找到订单 " + orderId + "，请核对订单号。");
    }

    /**
     * 查询订单的物流轨迹。
     *
     * @param orderId 订单号，形如 A1001
     * @return 物流轨迹描述
     */
    @Tool(description = "根据订单号查询物流轨迹与预计送达时间，用于回答包裹到哪了、什么时候到等问题")
    public String queryLogistics(String orderId) {
        return LOGISTICS.getOrDefault(orderId, "订单 " + orderId + "暂无物流信息（可能未发货或单号有误）。");
    }

    /**
     * 查询标准退换货规则（业务知识）。
     *
     * @return 退换货政策文案
     */
    @Tool(description = "查询小店标准退换货规则，当用户问到能否退货、退款政策时调用")
    public String getRefundPolicy() {
        return "7 天内支持无理由退换（商品完好、不影响二次销售）；生鲜与定制商品除外。";
    }
}
