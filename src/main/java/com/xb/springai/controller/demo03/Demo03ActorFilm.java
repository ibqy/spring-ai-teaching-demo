package com.xb.springai.controller.demo03;

import java.util.List;

/**
 * demo03：结构化输出 —— 让模型直接返回 Java 对象
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：大模型默认返回"一段文本"，但业务里我们常常想要一个规整的对象，
 * 比如"解析这份合同返回字段"、"抽取一段文字里的实体"。Spring AI 的
 * {@code call().entity(某个类型)} 会自动让模型以 JSON 形式输出，并反序列化成 Java 对象。</p>
 *
 * <p>这里用 Java 的 record（不可变数据载体）来定义返回结构，简洁且适合教学。</p>
 *
 * @param actor 演员姓名
 * @param movies 该演员主演的电影列表
 */
public record Demo03ActorFilm(
        String actor,
        List<String> movies) {
}