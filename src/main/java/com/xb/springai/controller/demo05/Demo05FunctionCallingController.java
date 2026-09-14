package com.xb.springai.controller.demo05;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo05：函数调用（Function Calling / Tool）
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：通过 {@code .tools(new WeatherTools())} 把工具交给 ChatClient。
 * 当用户问"杭州天气怎么样"，模型检测到需要动态数据 → 自动调用 {@code WeatherTools.getWeather("杭州")}
 * → 拿到结果 → 组织成自然语言回答。整个过程对调用方是透明的，一行代码即可实现"主动查询"。</p>
 */
@RestController
@RequestMapping("/api/demo05")
public class Demo05FunctionCallingController {

    private final ChatClient chatClient;

    public Demo05FunctionCallingController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GET /api/demo05/weather?city=杭州
     * 演示：让模型主动调用本地工具查询天气。
     */
    @GetMapping("/weather")
    public String weather(@RequestParam(defaultValue = "杭州") String city) {
        return this.chatClient.prompt()
                .user("请问 " + city + " 今天天气怎么样？")
                .tools(new WeatherTools())   // 关键：把工具注入这次调用
                .call()
                .content();
    }
}