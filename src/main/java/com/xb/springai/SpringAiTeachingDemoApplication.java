package com.xb.springai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI 教学演示项目 - 启动类
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p><b>教学知识点</b>：Spring Boot 应用入口。所有 demo 都由这个入口启动：
 * demo01 基础对话 → demo02 流式对话 → demo03 结构化输出 → demo04 提示词模板
 * → demo05 函数调用 → demo06 会话记忆 → demo07 系统角色 → demo08 RAG 检索增强</p>
 *
 * <p>@SpringBootApplication 是一个组合注解，等价于：
 * <ol>
 *     <li>@EnableAutoConfiguration：开启自动配置（Spring AI 的 ChatModel/EmbeddingModel 等在这里被自动装配）</li>
 *     <li>@ComponentScan：自动扫描该类所在包及子包的 @Component / @RestController / @Configuration</li>
 *     <li>@Configuration（元信息）：标记这是一个配置类</li>
 * </ol>
 * </p>
 */
@SpringBootApplication
public class SpringAiTeachingDemoApplication {

    public static void main(String[] args) {
        // 标准的 Spring Boot 启动方式
        SpringApplication.run(SpringAiTeachingDemoApplication.class, args);
    }
}