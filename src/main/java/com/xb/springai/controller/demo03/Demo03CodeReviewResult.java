package com.xb.springai.controller.demo03;

import java.util.List;

/**
 * demo03：结构化输出 —— 代码审查结果模型
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：同一个 {@code .entity()} 机制可以映射任意 Java 对象。
 * 这里用一个多字段 record 演示更复杂的结构化场景——让模型充当代码审查员，
 * 返回评分、问题列表、改进建议和总结。</p>
 *
 * <p>对比 {@link Demo03ActorFilm}（2 字段），本 record 有 5 个字段，
 * 展示了结构化输出在处理复杂业务对象时的能力。</p>
 *
 * @param language  编程语言
 * @param score     代码评分（0-100）
 * @param issues    发现的问题列表
 * @param suggestions 改进建议列表
 * @param summary   审查总结
 *
 * @author ibqy
 */
public record Demo03CodeReviewResult(
        String language,
        int score,
        List<String> issues,
        List<String> suggestions,
        String summary) {
}
