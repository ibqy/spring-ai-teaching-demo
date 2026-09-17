package com.xb.springai.controller.demo09;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * demo09 订单工具类的纯逻辑单元测试（离线可跑，不依赖大模型）。
 *
 * <p>作者：ibqy | 日期：2026-09-17</p>
 *
 * <p><b>教学知识点</b>：工具类的业务逻辑可以独立于 AI 模型进行测试，
 * 确保工具返回的数据准确可靠。</p>
 */
@DisplayName("OrderTools 订单工具测试")
class OrderToolsTest {

    private final OrderTools orderTools = new OrderTools();

    @Nested
    @DisplayName("订单查询")
    class QueryOrderTests {

        @Test
        @DisplayName("已知订单 A1001 返回已发货状态")
        void shouldReturnStatusForKnownOrderA1001() {
            String result = orderTools.queryOrder("A1001");

            assertThat(result)
                .contains("已发货")
                .contains("299");
        }

        @Test
        @DisplayName("已知订单 A1002 返回待付款状态")
        void shouldReturnStatusForKnownOrderA1002() {
            String result = orderTools.queryOrder("A1002");

            assertThat(result)
                .contains("待付款")
                .contains("88");
        }

        @Test
        @DisplayName("已知订单 B3003 返回已退款状态")
        void shouldReturnStatusForKnownOrderB3003() {
            String result = orderTools.queryOrder("B3003");

            assertThat(result)
                .contains("已退款")
                .contains("129");
        }

        @Test
        @DisplayName("未知订单返回友好提示")
        void shouldReturnFallbackForUnknownOrder() {
            String result = orderTools.queryOrder("X9999");

            assertThat(result)
                .contains("未找到订单")
                .contains("X9999");
        }
    }

    @Nested
    @DisplayName("退换货政策")
    class RefundPolicyTests {

        @Test
        @DisplayName("返回标准退换货规则")
        void shouldReturnStandardRefundPolicy() {
            String result = orderTools.getRefundPolicy();

            assertThat(result)
                .contains("7")
                .contains("无理由")
                .contains("退换");
        }

        @Test
        @DisplayName("政策包含例外说明")
        void shouldIncludeExceptions() {
            String result = orderTools.getRefundPolicy();

            assertThat(result)
                .containsAnyOf("除外", "不支持");
        }
    }

    @Nested
    @DisplayName("所有已知订单遍历")
    class AllOrdersTests {

        @Test
        @DisplayName("所有已知订单都能查到")
        void allKnownOrdersShouldBeQueryable() {
            String[] knownOrders = {"A1001", "A1002", "B3003"};

            for (String orderId : knownOrders) {
                String result = orderTools.queryOrder(orderId);
                assertThat(result)
                    .as("订单 '%s' 应能查到", orderId)
                    .doesNotContain("未找到订单");
            }
        }
    }
}
