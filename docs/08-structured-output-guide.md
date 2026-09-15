# 08 · 结构化输出

> **前置 Demo**：demo01 ~ demo04（掌握 ChatClient 和提示词模板）  
> **涉及 Demo**：demo03、demo04、demo10  
> **目标**：掌握 JSON → Java 实体的自动映射、单对象与批量列表输出、参数校验。

---

## 1. 为什么需要结构化输出？

大模型默认返回"一段文本"。但在业务中，我们通常需要**规整的结构化数据**：

- "请帮我解析这份合同，提取甲方、乙方、金额、日期"
- "请分析这段评论，返回情感分值、关键词列表"
- "请从这段聊天记录中抽取所有订单号、商品、金额"

手工解析模型输出的 JSON 字符串既繁琐又容易出错。Spring AI 的 `.entity()` 自动完成"JSON → Java 对象"的映射。

---

## 2. 基础：单对象映射（demo03）

### 2.1 定义目标实体

使用 Java `record`（不可变数据载体）定义输出结构：

```java
/**
 * @param actor  演员姓名
 * @param movies 该演员主演的电影列表
 */
public record Demo03ActorFilm(
        String actor,
        List<String> movies) {
}
```

### 2.2 调用 .entity()

```java
@GetMapping("/actor")
public Demo03ActorFilm actor(@RequestParam(defaultValue = "周星驰") String actor) {
    return this.chatClient.prompt()
            .user(u -> u
                    .text("请为演员 {actor} 自动生成他的 2 部代表作。")
                    .param("actor", actor))
            .call()
            .entity(Demo03ActorFilm.class);  // 关键：JSON → Java 自动映射
}
```

请求 `GET /api/demo03/actor?actor=周星驰`，返回：

```json
{
    "actor": "周星驰",
    "movies": ["大话西游", "喜剧之王"]
}
```

**底层原理**：Spring AI 提示模型以 JSON 格式输出，然后自动反序列化为 `Demo03ActorFilm` 对象。整个过程对调用方透明。

---

## 3. 进阶：批量列表映射（demo10）

单对象适合"一对一抽取"，但很多场景需要从一段文本中**批量抽取多条记录**。

### 3.1 定义实体

```java
/**
 * @param orderId 订单号
 * @param item    商品名
 * @param price   金额（元）
 * @param status  状态：已支付 / 待支付 / 已发货
 */
public record Demo10Order(
        String orderId,
        String item,
        double price,
        String status) {
}
```

### 3.2 使用 ParameterizedTypeReference 处理泛型

Java 的泛型在运行时会被擦除，因此**不能**直接用 `entity(List<Demo10Order>.class)`——这无法编译。必须借助 `ParameterizedTypeReference` 传递完整泛型信息：

```java
@GetMapping("/parse")
public List<Demo10Order> parse(@RequestParam String text) {
    return this.chatClient.prompt()
            .user(u -> u
                    .text("请从中抽取所有订单，字段包括 orderId(item商品/price价格/status状态)，" +
                          "只输出 JSON 数组，不要返回其它内容：{text}")
                    .param("text", text))
            .call()
            .entity(new ParameterizedTypeReference<List<Demo10Order>>() {});
    //              ↑ 匿名内部类携带完整泛型信息 ↑
}
```

请求 `GET /api/demo10/parse?text=订单A1001咖啡30元已支付，订单B2002茶叶88元待支付`，返回：

```json
[
    {"orderId":"A1001", "item":"咖啡", "price":30, "status":"已支付"},
    {"orderId":"B2002", "item":"茶叶", "price":88, "status":"待支付"}
]
```

---

## 4. 提示词模板与结构化输出的结合（demo04）

提示词模板和结构化输出并不互斥——可以先用模板组织提示词，再 `.entity()` 接收结构化结果：

```java
// demo04 的变体（教学演示）
@GetMapping("/poem")
public String poem(@RequestParam(defaultValue = "春天") String topic) {
    return this.chatClient.prompt()
            .user(u -> u
                    .text("请你写一首关于 {topic} 的五言绝句，要求押韵、有意境。")
                    .param("topic", topic))
            .call()
            .content();   // ← 这里取文本；换成 .entity(SomeClass.class) 即取对象
}
```

---

## 5. 提升结构化输出质量

### 5.1 提示词中明确结构

```
❌ 模糊：请分析这段评论
✅ 明确：请分析这段评论，返回 JSON，字段包括：
   sentiment（正面/负面/中性）、score（1-10）、keywords（关键词列表）。
   只输出 JSON，不要附带其它文字。
```

### 5.2 使用结构化输出增强

Spring AI 支持 `useProviderStructuredOutput()`，将 JSON Schema 以 API 级约束下发：

```java
.chatClient.prompt()
    .user("请为演员 " + actor + " 自动生成 2 部代表作。")
    .call()
    .entity(Demo03ActorFilm.class, spec -> spec
            .useProviderStructuredOutput()   // 使用模型的结构化输出能力
            .validateSchema()                // 输出后校验 JSON Schema
    );
```

部分模型（如 GPT-4o）原生支持结构化输出，精度比纯提示词引导更高。

---

## 6. 数据类型支持

| Java 类型 | 说明 |
|-----------|------|
| `String` | 文本 |
| `int` / `long` / `double` | 数值 |
| `boolean` | 布尔 |
| `List<T>` | 嵌套列表（如 `List<String>`） |
| 嵌套 record | 支持多层嵌套（如 `Order { List<Item> items }`） |
| `Enum` | 自动映射枚举值 |

---

## 7. 异常处理

| 场景 | 表现 | 处理 |
|------|------|------|
| JSON 格式不合法 | 反序列化异常 | 在提示词中加"只输出 JSON"的约束 |
| 字段缺失 | 对应字段为 null | 使用 `@NotNull` 校验 + 重试机制 |
| 字段类型不匹配 | 类型转换异常 | 检查 record 字段类型与实际返回值一致 |
| 泛型擦除 | 运行时类型丢失 | 用 `ParameterizedTypeReference` 代替 `.class` |

---

## 8. 单对象 vs 列表对比

| 维度 | 单对象 | 列表 |
|------|--------|------|
| Controller 返回 | `Demo03ActorFilm` | `List<Demo10Order>` |
| API | `.entity(Demo03ActorFilm.class)` | `.entity(new ParameterizedTypeReference<List<Demo10Order>>() {})` |
| JSON 格式 | `{...}` | `[{...}, {...}]` |
| 适用场景 | 实体抽取、信息补全 | 批量抽取、数据清洗、报表生成 |
| 对应 Demo | demo03 | demo10 |

---

## 9. 测试建议

结构化输出的单元测试可以不依赖真实大模型（参考项目中的 `Demo10OrderTest`）：

```java
@Test
void testOrderParsing() {
    // 用 ObjectMapper 直接验证 JSON 反序列化逻辑
    String json = "[{\"orderId\":\"A1001\",\"item\":\"咖啡\",\"price\":30,\"status\":\"已支付\"}]";
    List<Demo10Order> orders = objectMapper.readValue(json,
            new TypeReference<List<Demo10Order>>() {});
    assertEquals(1, orders.size());
    assertEquals("A1001", orders.get(0).orderId());
}
```

---

## 下一步

掌握结构化输出后，阅读生产部署清单：[09 · 生产部署清单](./09-production-checklist.md)。
