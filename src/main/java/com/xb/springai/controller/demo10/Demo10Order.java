package com.xb.springai.controller.demo10;

import java.util.List;

/**
 * demo10：批量结构化输出 —— 一条消息里抽取多条记录
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>示例业务</b>：把用户一段杂乱文本里的多个订单，一次性抽成规整的 Java 对象列表。</p>
 *
 * @param orderId  订单号
 * @param item     商品名
 * @param price    金额（元）
 * @param status   状态：已支付 / 待支付 / 已发货
 *
 * @author ibqy
 */
public record Demo10Order(
        String orderId,
        String item,
        double price,
        String status) {

    /**
     * 一个便捷的静态示例，供测试：期望从原始文本中解析出一批这样的对象。
     * 这里仅用于演示数据结构，实际数据由模型返回后自动填充。
     */
    public static List<Demo10Order> exampleList() {
        return List.of();
    }
}