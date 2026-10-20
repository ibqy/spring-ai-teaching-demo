package com.xb.springai.controller.demo21;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * demo21 Token 计量 Advisor 的单元测试（离线可跑，不依赖大模型）。
 *
 * <p>作者：ibqy | 日期：2026-10-16</p>
 *
 * <p><b>教学知识点</b>：横切逻辑（记账/统计）最容易出隐蔽 bug——并发累加、
 * 空值容错、边界重置。把「记账核心逻辑」抽成一个可独立构造
 * {@link ChatResponse} 的方法，就能用普通单元测试覆盖这些分支，
 * 这是把 Advisor 写「可测试」的典型手法。</p>
 */
@DisplayName("Demo21TokenAccountingAdvisor 用量计量测试")
class Demo21TokenAccountingAdvisorTest {

    /** 不带单价的计量器：只统计 Token */
    private Demo21TokenAccountingAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new Demo21TokenAccountingAdvisor(0, 0);
    }

    /** 构造一个带用量元数据的模型响应（模拟真实大模型返回） */
    private ChatResponse responseWithUsage(int promptTokens, int completionTokens) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("test-model")
                .usage(new DefaultUsage(promptTokens, completionTokens))
                .build();
        Generation generation = new Generation(new AssistantMessage("测试回答"));
        return new ChatResponse(List.of(generation), metadata);
    }

    @Nested
    @DisplayName("正常记账")
    class NormalAccountingTests {

        @Test
        @DisplayName("记录一次调用后计数器正确累加")
        void shouldAccumulateTokensAfterOneRecord() {
            advisor.record(responseWithUsage(120, 45));

            var snapshot = advisor.snapshot();
            assertThat(snapshot).containsEntry("callCount", 1L)
                    .containsEntry("meteredCallCount", 1L)
                    .containsEntry("promptTokens", 120L)
                    .containsEntry("completionTokens", 45L)
                    .containsEntry("totalTokens", 165L);
        }

        @Test
        @DisplayName("多次调用累计正确")
        void shouldAccumulateAcrossMultipleRecords() {
            advisor.record(responseWithUsage(100, 50));
            advisor.record(responseWithUsage(200, 25));

            var snapshot = advisor.snapshot();
            assertThat(snapshot).containsEntry("callCount", 2L)
                    .containsEntry("meteredCallCount", 2L)
                    .containsEntry("promptTokens", 300L)
                    .containsEntry("completionTokens", 75L)
                    .containsEntry("totalTokens", 375L)
                    // 平均 = 375 / 2 = 187（整数除法）
                    .containsEntry("avgTokensPerMeteredCall", 187L);
        }

        @Test
        @DisplayName("清零后所有计数归零")
        void shouldResetAllCounters() {
            advisor.record(responseWithUsage(100, 50));
            advisor.reset();

            var snapshot = advisor.snapshot();
            assertThat(snapshot).containsEntry("callCount", 0L)
                    .containsEntry("meteredCallCount", 0L)
                    .containsEntry("promptTokens", 0L)
                    .containsEntry("completionTokens", 0L)
                    .containsEntry("totalTokens", 0L)
                    .containsEntry("avgTokensPerMeteredCall", 0L);
        }
    }

    @Nested
    @DisplayName("容错分支")
    class FaultToleranceTests {

        @Test
        @DisplayName("响应没有显式用量时按 0 Token 安全记账，不抛异常")
        void shouldTolerateResponseWithoutUsage() {
            // Spring AI 2.x 的 ChatResponseMetadata.builder() 即使不调用 .usage()，
            // 也会填充一个空 Usage（字段为 null）；单参数构造的 ChatResponse 同理。
            // 计量器必须把这种「空用量」当作 0 Token 处理，而不是抛 NPE。
            Generation generation = new Generation(new AssistantMessage("回答"));
            ChatResponse response = new ChatResponse(List.of(generation),
                    ChatResponseMetadata.builder().model("test-model").build());

            advisor.record(response);

            var snapshot = advisor.snapshot();
            assertThat(snapshot).containsEntry("callCount", 1L)
                    .containsEntry("meteredCallCount", 1L)
                    .containsEntry("promptTokens", 0L)
                    .containsEntry("completionTokens", 0L)
                    .containsEntry("totalTokens", 0L);
        }

        @Test
        @DisplayName("单参数构造的响应带空 usage（0 Token），安全记账不抛异常")
        void shouldTolerateEmptyUsageFromSingleArgConstructor() {
            // Spring AI 的 ChatResponse(List<Generation>) 单参数构造器会填充空 usage（0/0），
            // 计量器应把它当作「已计量但 0 Token」处理，而不是报错
            Generation generation = new Generation(new AssistantMessage("回答"));
            ChatResponse response = new ChatResponse(List.of(generation));

            advisor.record(response);

            var snapshot = advisor.snapshot();
            assertThat(snapshot).containsEntry("callCount", 1L)
                    .containsEntry("promptTokens", 0L)
                    .containsEntry("completionTokens", 0L)
                    .containsEntry("totalTokens", 0L);
        }

        @Test
        @DisplayName("metadata 为 null 时安全跳过")
        void shouldTolerateNullMetadata() {
            Generation generation = new Generation(new AssistantMessage("回答"));
            ChatResponse response = new ChatResponse(List.of(generation), null);

            advisor.record(response);

            assertThat(advisor.snapshot()).containsEntry("callCount", 1L);
        }

        @Test
        @DisplayName("usage 存在但字段为 null 时安全跳过对应计数")
        void shouldTolerateNullUsageFields() {
            ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(null, null))
                    .build();
            Generation generation = new Generation(new AssistantMessage("回答"));
            ChatResponse response = new ChatResponse(List.of(generation), metadata);

            advisor.record(response);

            var snapshot = advisor.snapshot();
            // 拿到了 usage（metered +1），但 Token 数为 null，不累加
            assertThat(snapshot).containsEntry("callCount", 1L)
                    .containsEntry("meteredCallCount", 1L)
                    .containsEntry("promptTokens", 0L)
                    .containsEntry("completionTokens", 0L);
        }
    }

    @Nested
    @DisplayName("成本估算")
    class CostEstimationTests {

        @Test
        @DisplayName("按配置单价正确估算成本")
        void shouldEstimateCostWithConfiguredPrices() {
            // 输入 2 元/百万 Token，输出 8 元/百万 Token
            Demo21TokenAccountingAdvisor pricedAdvisor =
                    new Demo21TokenAccountingAdvisor(2.0, 8.0);
            // 各 100 万 Token：成本 = 1 * 2 + 1 * 8 = 10 元
            pricedAdvisor.record(responseWithUsage(1_000_000, 1_000_000));

            var snapshot = pricedAdvisor.snapshot();
            assertThat((Double) snapshot.get("estimatedCostYuan")).isEqualTo(10.0);
        }

        @Test
        @DisplayName("未配置单价时成本为 0 并给出提示")
        void shouldReturnZeroCostWhenNoPriceConfigured() {
            advisor.record(responseWithUsage(1_000_000, 1_000_000));

            var snapshot = advisor.snapshot();
            assertThat((Double) snapshot.get("estimatedCostYuan")).isEqualTo(0.0);
            assertThat(snapshot.get("priceNote").toString()).contains("未配置模型单价");
        }
    }
}
