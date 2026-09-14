package com.xb.springai.controller.demo05;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * demo05 工具类的纯逻辑单元测试（离线可跑，不依赖大模型）。
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：函数调用里的"工具函数"本质就是普通 Java 方法，
 * 可以先像测普通方法一样把它测好，再让模型调用它，从而保证数据源头可靠。</p>
 */
class WeatherToolsTest {

    private final WeatherTools weatherTools = new WeatherTools();

    @Test
    void shouldReturnKnownCityWeather() {
        // 已知城市的天气应能查到
        String result = weatherTools.getWeather("杭州");
        assertThat(result).contains("晴").contains("26");
    }

    @Test
    void shouldFallbackWhenCityUnknown() {
        // 未知城市应返回友好提示，而不是报错
        String result = weatherTools.getWeather("东京");
        assertThat(result).contains("暂无").contains("东京");
    }
}