# 11 · 多模态实战

> **前置 Demo**：demo01、demo03（掌握 ChatClient 基础与结构化输出）
> **涉及 Demo**：demo17
> **目标**：掌握多模态输入（图片理解）与输出（图片生成）的完整用法。

---

## 1. 什么是多模态？

现代旗舰模型（GPT-4o、Claude 4、Gemini 2、Qwen-VL 等）早已不是"文本进文本出"——它们原生支持**多模态**：

| 方向 | 说明 | 典型场景 |
|------|------|----------|
| **多模态输入（Vision）** | 图片 + 文字一起发给模型，模型"看懂"图片 | 图片描述、发票/证件识别、商品审核、截图问答 |
| **多模态输出（Image Gen）** | 按文字描述生成图片 | 海报设计、封面生成、创意绘图 |

本 Demo 演示两个最常用场景：**视觉理解** + **图片生成**。

---

## 2. 视觉理解 — Media 类

Spring AI 用 `Media` 封装非文本输入（图片、音频），作为 UserMessage 的"媒体附件"：

```java
chatClient.prompt()
    .user(u -> u
        .text(question)                                    // 文字问题
        .media(Media.builder(mime, new UrlResource(url))    // 图片附件
               .build()))
    .call()
    .content();
```

### 2.1 支持的图片来源

| 来源 | 写法 | 说明 |
|------|------|------|
| HTTP URL | `new UrlResource("https://.../demo.png")` | 最常用，模型直接取图 |
| 本地文件 | `new ClassPathResource("img/demo.png")` | Spring AI 自动转 base64 |
| Base64 字符 | `Media.builder(mime, dataUri)` | 图片已在内存中的场景 |

### 2.2 MIME 类型处理

图片 URL 需要指定正确的 MIME 类型（png / jpeg / gif / webp）。demo17 中按 URL 后缀做了推断：

```java
MimeType mime = MimeTypeUtils.IMAGE_PNG;
String lower = imageUrl.toLowerCase(Locale.ROOT);
if (lower.contains(".jpg") || lower.contains(".jpeg")) {
    mime = MimeTypeUtils.IMAGE_JPEG;
} else if (lower.contains(".gif")) {
    mime = MimeTypeUtils.IMAGE_GIF;
} else if (lower.contains(".webp")) {
    mime = MimeTypeUtils.parseMimeType("image/webp");
}
```

### 2.3 运行示例

```bash
# 图片描述
curl "http://localhost:8080/api/demo17/vision?imageUrl=https://upload.wikimedia.org/.../png&question=请详细描述这张图片的内容"

# 组合结构化输出（先看图，再抽字段）
curl "http://localhost:8080/api/demo17/vision?imageUrl=https://.../receipt.jpg&question=这是发票，请提取金额和开票日期"
```

**底层原理**：OpenAI 兼容协议把带 `media` 的 UserMessage 转成 `image_url` 类型的 content part（或 `data:image/...;base64,...`），模型侧完成视觉推理。

---

## 3. 图片生成 — ImageModel

Spring AI 的 `ImageModel` 统一封装了图片生成 API（OpenAI DALL·E / Stability 等）：

```java
ImageResponse response = imageModel.call(
    new ImagePrompt(prompt, OpenAiImageOptions.builder()
        .model("dall-e-3")
        .build()));

String url = response.getResult().getOutput().getUrl();       // 图片 URL
String b64 = response.getResult().getOutput().getB64Json();    // 或 base64
```

### 3.1 OpenAiImageOptions 常用参数

| 参数 | 默认 | 说明 |
|------|------|------|
| `model` | `dall-e-3` | 生成模型 |
| `n` | 1 | 一次生成的图片张数 |
| `size` | `1024x1024` | 分辨率（1024x1024 / 1792x1024 / 1024x1792） |
| `quality` | `standard` | `standard` / `hd` |
| `style` | `vivid` | `vivid`（生动）/ `natural`（自然） |
| `response_format` | `url` | `url` / `b64_json` |

### 3.2 运行示例

```bash
curl "http://localhost:8080/api/demo17/gen?prompt=一只戴着贝雷帽的柴犬，油画风格"
# → {"prompt":"一只戴着贝雷帽的柴犬，油画风格","model":"dall-e-3","url":"https://...","b64Length":0}
```

### 3.3 ⚠️ 服务商兼容性（重要）

| 服务商 | 图片理解 | 图片生成 |
|--------|----------|----------|
| OpenAI（gpt-4o 系列） | ✅ | ✅（dall-e-3） |
| DeepSeek | ❌（纯文本模型） | ❌ |
| 智谱 GLM-4V | ✅ | ✅（CogView，需换 base-url 指向专用端点） |
| 通义千问 qwen-vl / qwen-vl-max | ✅（OpenAI 兼容模式） | ✅（wanx，需专用端点） |
| 本地 Ollama（llava / qwen2-vl） | ✅ | ❌ |

demo17 对不支持的服务做了**友好降级**：`ImageModel` 不可用时 `/gen` 返回错误提示，不会抛异常。

---

## 4. 多模态实战场景

| 场景 | 组合 | 关键点 |
|------|------|--------|
| 发票/证件识别 | Vision + 结构化输出 | 提示词要求"只输出 JSON"，配合 `.entity()` 反序列化 |
| 商品内容审核 | Vision + Advisor | 截图 → 模型判断违规 → 输出结构化标签 |
| 客服截图问答 | Vision + 记忆 + RAG | 用户发截图，模型结合知识库解答 |
| 营销素材生成 | 文案生成 + Image Gen | 文字模型写提示词，再交给 ImageModel 生成配图 |
| 工业质检 | Vision + 函数调用 | 图像 → 异常判断 → 调用上报工具 |

### 组合示例：发票识别（Vision + 结构化输出）

```java
public record Invoice(String invoiceNo, double amount, String date) {}

Invoice invoice = chatClient.prompt()
    .user(u -> u
        .text("请从这张发票图片中提取字段，只输出 JSON：" +
              "invoiceNo（发票号）、amount（金额）、date（开票日期）。")
        .media(buildMedia(imageUrl)))
    .call()
    .entity(Invoice.class);
```

---

## 5. 成本与限制提示

1. **多模态计费按图片大小**：视觉理解时图片会消耗 token，一张高清图可能相当于数百 token
2. **分辨率折算规则**（OpenAI 兼容协议）：图片按 `宽×高 / 750` 折算 token 数
3. **限制**：单次请求的图片数量通常 ≤ 10 张；单图 ≤ 20MB
4. **最佳实践**：低延迟场景用 `detail: "low"`（Spring AI 中通过 Options 传 `imageDetail`）

---

## 6. 小结

- **多模态输入**：`Media` + `.media()` 把图片挂进 UserMessage，模型"看图说话"
- **多模态输出**：`ImageModel` + `OpenAiImageOptions` 生成图片
- **降级处理**：服务商不支持时返回友好错误，而非抛异常
- **组合威力**：Vision + 结构化输出 = 发票识别；Vision + RAG = 截图问答
- **成本意识**：图片按尺寸折算 token，高清图很贵，注意 `detail` 参数

---

## 下一步

恭喜掌握全部进阶能力！回顾完整学习路线：

```
01 快速开始 → 02 核心概念 → 03 RAG 深入 → 04 函数调用
    → 05 Agent Skills → 06 ETL 管道 → 07 Advisor → 08 结构化输出
    → 09 生产部署 → 10 MCP 客户端 → 11 多模态
```

建议结合 demo11（多轮智能客服）+ demo16（MCP）+ demo17（多模态）自行组合，构建"能看图、能调工具、能读私有知识"的全能智能体。
