package com.xb.springai.controller.demo10;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo10：批量结构化输出 —— 从一段文本抽取多条订单记录
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>进阶点（相较 demo03）</b>：
 * <ol>
 *   <li>demo03 用 {@code entity(类名)} 让模型返回"一个对象"。</li>
 *   <li>demo10 用 {@link ParameterizedTypeReference}{@code <List<Demo10Order>>} 让模型返回
 *       "一串对象"，适合批量抽取、报表、数据清洗等场景。</li>
 *   <li>核心：{@code call().entity(new ParameterizedTypeReference<List<Demo10Order>>(){})}，
 *       因为泛型在运行时会被擦除，必须借助 TypeReference 传递完整类型信息。</li>
 * </ol></p>
 *
 * <p><b>实战价值</b>：用户/系统喂进来一段非结构化文本（如聊天记录、客服工单、日志），
 * AI 可直接抽出规整的多条业务数据，进入下游数据库或报表管线。</p>
 */
@RestController
@RequestMapping("/api/demo10")
public class Demo10ListStructuredOutputController {

    private final ChatClient chatClient;

    public Demo10ListStructuredOutputController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * GET /api/demo10/parse?text=订单A1001咖啡30元已支付，订单B2002茶叶88元待支付
     * 返回 JSON 数组：<code>[{"orderId":"A1001","item":"咖啡",...}, ...]</code>
     */
    @GetMapping("/parse")
    public List<Demo10Order> parse(@RequestParam(defaultValue = "订单A1001买了咖啡30元已支付，订单B2002买了茶叶88元待支付，订单C3003买了杯子25元已发货")
                                   String text) {
        // 关键：用 ParameterizedTypeReference 携带 List<Demo10Order> 的完整泛型信息
        return this.chatClient.prompt()
                .user(u -> u
                        .text("请从中抽取所有订单，字段包括 orderId(item商品/price价格/status状态)，只输出 JSON 数组，不要返回其它内容：{text}")
                        .param("text", text))
                .call()
                .entity(new ParameterizedTypeReference<List<Demo10Order>>() {});
    }
}