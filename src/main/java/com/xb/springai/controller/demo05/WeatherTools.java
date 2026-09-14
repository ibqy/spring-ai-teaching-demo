package com.xb.springai.controller.demo05;

import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * demo05 用到的"工具"：天气查询
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：什么是函数调用（Function / Tool Calling）？
 * 大模型本身不知道'实时天气'这种动态数据，它能做的是——在合适的时机"请求"调用我们提供的函数，
 * 拿到结果后再组织语言回答。</p>
 *
 * <p>用 {@link Tool} 注解标记一个方法，Spring AI 会自动把方法名、参数、描述生成
 * "函数定义（JSON Schema）"下发给模型；模型判断需要时就会回调它。</p>
 *
 * <p>这里模拟一份内存中的天气数据，真实项目里改成查天气 API / 查数据库即可。</p>
 */
@Component
public class WeatherTools {

    /** 模拟的天气数据库：城市 -> 天气描述 */
    private static final Map<String, String> WEATHER = Map.of(
            "杭州", "晴，26℃",
            "北京", "多云，22℃",
            "上海", "小雨，24℃",
            "广州", "雷阵雨，29℃"
    );

    /**
     * 查询指定城市的当前天气。
     *
     * <p>{@code description} 非常关键：它告诉大模型"什么时候该用这个工具、参数是什么含义"，
     * 写好描述，模型才能正确触发调用。</p>
     *
     * @param city 城市名称
     * @return 该城市的天气描述
     */
    @Tool(description = "查询指定城市的当前天气，用于回答天气相关问题")
    public String getWeather(String city) {
        return WEATHER.getOrDefault(city, "对不起，暂无 " + city + " 的天气数据。");
    }
}