package com.xb.springai.controller.demo17;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * demo17：多模态 —— 视觉理解（图生文）+ 图片生成
 *
 * <p>作者：ibqy | 日期：2026-09-15</p>
 *
 * <p><b>教学知识点</b>：大模型早已不只是"文本进文本出"，现代旗舰模型普遍具备
 * 多模态能力。本 Demo 演示两个最常用的多模态场景：</p>
 * <ol>
 *     <li><b>视觉理解（Vision）</b>：把一张图片（URL）作为消息的一部分发给模型，
 *         模型"看懂"图片并回答问题。Spring AI 用 {@link Media} 封装图片资源。</li>
 *     <li><b>图片生成（Image Generation）</b>：用 {@link ImageModel} 按文字描述
 *         生成图片（本示例默认 dall-e-3）。注意：仅 OpenAI 官方或支持图片生成的
 *         兼容服务可用，DeepSeek / 通义千问等纯对话服务不支持，会返回友好错误。</li>
 * </ol>
 *
 * <p><b>演示问题示例</b>：</p>
 * <pre>
 * # 视觉理解：让模型描述一张图片
 * GET /api/demo17/vision?imageUrl=https://.../demo.png&question=请描述这张图片的内容
 *
 * # 图片生成：按文字描述生成图片（OpenAI 系模型）
 * GET /api/demo17/gen?prompt=一只戴着贝雷帽的柴犬，油画风格
 * </pre>
 */
@RestController
@RequestMapping("/api/demo17")
public class Demo17MultimodalController {

    private final ChatClient chatClient;

    /** 图片生成模型：由 openai 启动器自动配置（仅支持图片生成的服务商才可用） */
    private final ImageModel imageModel;

    /** 图片生成模型名，可在 application.yml 中通过环境变量覆盖 */
    private final String imageGenModel;

    public Demo17MultimodalController(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) ImageModel imageModel,
            @Value("${OPENAI_IMAGE_MODEL:dall-e-3}") String imageGenModel) {
        this.chatClient = chatClientBuilder.build();
        this.imageModel = imageModel;
        this.imageGenModel = imageGenModel;
    }

    /**
     * GET /api/demo17/vision?imageUrl=...&question=...
     * 视觉理解：把图片作为 UserMessage 的媒体附件，让模型"看图说话"。
     */
    @GetMapping("/vision")
    public String vision(
            @RequestParam String imageUrl,
            @RequestParam(defaultValue = "请详细描述这张图片的内容") String question) {
        return this.chatClient.prompt()
                .user(u -> u
                        .text(question)
                        .media(buildMedia(imageUrl)))
                .call()
                .content();
    }

    /**
     * GET /api/demo17/gen?prompt=一只戴着贝雷帽的柴犬，油画风格
     * 图片生成：按文字描述调用 ImageModel 生成图片，返回图片 URL 与 base64 长度。
     */
    @GetMapping("/gen")
    public Map<String, Object> generate(@RequestParam(defaultValue = "一只戴着贝雷帽的柴犬，油画风格") String prompt) {
        if (imageModel == null) {
            return Map.of(
                    "error", "当前未配置图片生成能力（服务商不支持 ImageModel API）",
                    "hint", "仅 OpenAI 官方或支持图片生成的兼容服务可调用本接口");
        }
        try {
            ImageResponse response = imageModel.call(
                    new ImagePrompt(prompt, OpenAiImageOptions.builder()
                            .model(imageGenModel)
                            .build()));
            String url = response.getResult().getOutput().getUrl();
            String b64 = response.getResult().getOutput().getB64Json();
            return Map.of(
                    "prompt", prompt,
                    "model", imageGenModel,
                    "url", url == null ? "" : url,
                    "b64Length", b64 == null ? 0 : b64.length());
        } catch (Exception e) {
            return Map.of("error", "图片生成失败：" + e.getMessage());
        }
    }

    /**
     * 根据图片 URL 后缀推断 MIME 类型（png / jpeg / gif / webp，默认 png）。
     */
    private Media buildMedia(String imageUrl) {
        MimeType mime = MimeTypeUtils.IMAGE_PNG;
        String lower = imageUrl.toLowerCase(Locale.ROOT);
        if (lower.contains(".jpg") || lower.contains(".jpeg")) {
            mime = MimeTypeUtils.IMAGE_JPEG;
        } else if (lower.contains(".gif")) {
            mime = MimeTypeUtils.IMAGE_GIF;
        } else if (lower.contains(".webp")) {
            mime = MimeTypeUtils.parseMimeType("image/webp");
        }
        return Media.builder(mime, new UrlResource(imageUrl)).build();
    }
}
