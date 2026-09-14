package com.xb.springai.controller.demo10;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * demo10 相关的一个轻量单元测试示例。
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p>真正的 JSON 抽取依赖真实大模型，无法离线测试。这里用一个简易"模拟解析器"
 * 验证核心数据结构可用，作为"如何为接入了 AI 的控制器写测试"的思路示范——</p>
 * <ul>
 *   <li>把"模型无关"的纯逻辑抽出来单独测</li>
 *   <li>依赖模型的链路则用 mock / 集成测试覆盖</li>
 * </ul>
 */
class Demo10OrderTest {

    @Test
    void exampleListShouldBeEmptyByDefault() {
        assertThat(Demo10Order.exampleList()).isEmpty();
    }

    @Test
    void orderRecordShouldExposeAllFields() {
        // 演示 record 的解析与访问（这里手动构造，真实场景由模型返回后自动填充）
        Demo10Order order = new Demo10Order("A1001", "咖啡", 30.0, "已支付");
        assertThat(order.orderId()).isEqualTo("A1001");
        assertThat(order.item()).isEqualTo("咖啡");
        assertThat(order.price()).isEqualTo(30.0);
        assertThat(order.status()).isEqualTo("已支付");
    }
}