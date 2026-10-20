package com.xb.springai.controller.demo20;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * demo20 MCP 服务端工具类的纯逻辑单元测试（离线可跑，不依赖大模型与 MCP 协议）。
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：MCP 服务端发布的也是普通 Spring Bean 的工具方法，
 * 协议层（自动配置）与业务层（本类）解耦——所以工具逻辑完全可以像
 * 普通单元测试一样离线验证，不需要起 MCP 客户端。</p>
 */
@DisplayName("Demo20ShopTools MCP 服务端工具测试")
class Demo20ShopToolsTest {

    private final Demo20ShopTools tools = new Demo20ShopTools();

    @Nested
    @DisplayName("订单查询")
    class QueryOrderTests {

        @Test
        @DisplayName("已知订单 A1001 返回已发货状态")
        void shouldReturnStatusForKnownOrderA1001() {
            assertThat(tools.queryOrder("A1001"))
                    .contains("已发货")
                    .contains("299");
        }

        @Test
        @DisplayName("未知订单返回友好提示（不抛异常，让模型能解释）")
        void shouldReturnFallbackForUnknownOrder() {
            assertThat(tools.queryOrder("X9999"))
                    .contains("未找到订单")
                    .contains("X9999");
        }
    }

    @Nested
    @DisplayName("物流轨迹")
    class QueryLogisticsTests {

        @Test
        @DisplayName("已知订单 A1001 返回物流轨迹")
        void shouldReturnLogisticsForKnownOrderA1001() {
            assertThat(tools.queryLogistics("A1001"))
                    .contains("杭州转运中心");
        }

        @Test
        @DisplayName("未知订单返回暂无物流信息")
        void shouldReturnFallbackForUnknownOrder() {
            assertThat(tools.queryLogistics("X9999"))
                    .contains("暂无物流信息");
        }
    }

    @Nested
    @DisplayName("退换货政策")
    class RefundPolicyTests {

        @Test
        @DisplayName("返回标准退换货规则")
        void shouldReturnStandardRefundPolicy() {
            assertThat(tools.getRefundPolicy())
                    .contains("7")
                    .contains("无理由")
                    .contains("退换");
        }
    }
}
