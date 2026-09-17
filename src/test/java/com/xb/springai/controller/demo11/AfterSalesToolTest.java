package com.xb.springai.controller.demo11;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * demo11 售后工具类的纯逻辑单元测试（离线可跑，不依赖大模型）。
 *
 * <p>作者：ibqy | 日期：2026-09-17</p>
 *
 * <p><b>教学知识点</b>：工具类的业务逻辑可以独立于 AI 模型进行测试，
 * 确保工具返回的数据准确可靠。</p>
 */
@DisplayName("AfterSalesTool 售后工具测试")
class AfterSalesToolTest {

    private final AfterSalesTool afterSalesTool = new AfterSalesTool();

    @Nested
    @DisplayName("已知品类售后政策")
    class KnownCategoryTests {

        @Test
        @DisplayName("咖啡豆：食品类，开封后不支持退换")
        void shouldReturnPolicyForCoffeeBeans() {
            String result = afterSalesTool.getAfterSalePolicy("咖啡豆");

            assertThat(result)
                .contains("咖啡豆")
                .contains("食品")
                .contains("开封");
        }

        @Test
        @DisplayName("器具：7天无理由，1年保修")
        void shouldReturnPolicyForTools() {
            String result = afterSalesTool.getAfterSalePolicy("器具");

            assertThat(result)
                .contains("器具")
                .contains("7天")
                .contains("1年");
        }

        @Test
        @DisplayName("周边：7天无理由退换")
        void shouldReturnPolicyForAccessories() {
            String result = afterSalesTool.getAfterSalePolicy("周边");

            assertThat(result)
                .contains("周边")
                .contains("7天");
        }
    }

    @Nested
    @DisplayName("未知品类处理")
    class UnknownCategoryTests {

        @Test
        @DisplayName("未知品类返回友好提示")
        void shouldReturnFallbackForUnknownCategory() {
            String result = afterSalesTool.getAfterSalePolicy("未知品类");

            assertThat(result)
                .contains("品类");
        }

        @Test
        @DisplayName("空字符串返回友好提示")
        void shouldReturnFallbackForEmptyCategory() {
            String result = afterSalesTool.getAfterSalePolicy("");

            assertThat(result)
                .contains("品类");
        }
    }

    @Nested
    @DisplayName("所有已知品类遍历")
    class AllKnownCategoriesTests {

        @Test
        @DisplayName("所有已知品类都有对应政策")
        void allKnownCategoriesShouldHavePolicy() {
            String[] knownCategories = {"咖啡豆", "器具", "周边"};

            for (String category : knownCategories) {
                String result = afterSalesTool.getAfterSalePolicy(category);
                assertThat(result)
                    .as("品类 '%s' 应有具体政策", category)
                    .isNotEmpty()
                    .doesNotContain("请提供具体品类");
            }
        }
    }
}
