package com.xb.springai.controller.demo15;

import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * demo15：Agent Skills + Nacos 配置热更新与回滚。
 *
 * <p>作者：ibqy | 日期：2026-09-15</p>
 *
 * <p><b>教学知识点</b>：将 demo14 的静态 classpath 技能定义，升级为 Nacos 配置中心动态管理：</p>
 * <ol>
 *     <li><b>Nacos 配置中心</b>：SKILL.md 内容存储在 Nacos（dataId = skill-kb-reader / skill-script-runner），
 *          通过 Nacos 控制台修改即可更新技能指令，无需重启应用。</li>
 *     <li><b>热更新机制</b>：{@link NacosSkillConfigManager} 通过 Nacos Listener 监听配置变更，
 *          变更后自动重建 ChatClient，新技能即刻生效。</li>
 *     <li><b>配置回滚</b>：Nacos 控制台保留每次发布的历史版本，出问题时在控制台点击"回滚"
 *          即恢复上一版技能定义，Java 端无需改任何代码。</li>
 *     <li><b>降级策略</b>：当 {@code nacos.enabled=false} 或 Nacos 不可用时，
 *          自动回退到 classpath:skills/ 静态技能定义，保证服务不中断。</li>
 * </ol>
 *
 * <p><b>启动前准备</b>：</p>
 * <pre>
 * # 1) 启动 Nacos（Docker 一键）
 * docker run -d --name nacos -p 8848:8848 -p 9848:9848 \
 *   -e MODE=standalone nacos/nacos-server:v2.4.0
 *
 * # 2) 推送默认技能定义到 Nacos
 * bash scripts/nacos-init.sh
 *
 * # 3) 修改 application.yml：nacos.enabled=true
 * </pre>
 *
 * <p><b>热更新演示流程</b>：</p>
 * <pre>
 * # 步骤1——查看当前技能
 * curl http://localhost:8080/api/demo15/skills
 *
 * # 步骤2——在 Nacos 控制台修改 skill-kb-reader 内容（例如改促销规则），发布
 *
 * # 步骤3——验证热更新生效（日志会输出"ChatClient 重建完成"）
 * curl "http://localhost:8080/api/demo15/ask?question=促销活动规则是什么"
 *
 * # 步骤4——在 Nacos 控制台点击"历史版本"→"回滚"，恢复上版技能定义
 * </pre>
 */
@RestController
@RequestMapping("/api/demo15")
public class Demo15NacosSkillsController {

    private static final Logger log = LoggerFactory.getLogger(Demo15NacosSkillsController.class);

    private final NacosSkillConfigManager configManager;
    private final ChatClient.Builder chatClientBuilder;
    private final boolean nacosEnabled;
    private volatile ChatClient chatClient;

    public Demo15NacosSkillsController(
            ChatClient.Builder chatClientBuilder,
            @Value("${nacos.server-addr:127.0.0.1:8848}") String serverAddr,
            @Value("${nacos.enabled:false}") boolean nacosEnabled) {

        this.chatClientBuilder = chatClientBuilder;
        this.nacosEnabled = nacosEnabled;

        if (nacosEnabled) {
            try {
                this.configManager = new NacosSkillConfigManager(serverAddr);
                // 注册配置变更回调——Nacos 配置变了就重建 ChatClient
                this.configManager.onChange(this::rebuildChatClient);
            } catch (NacosException e) {
                log.error("Nacos 连接失败（serverAddr={}），请检查 Nacos 是否已启动", serverAddr, e);
                throw new RuntimeException("Nacos 连接失败，请先启动 Nacos 或将 nacos.enabled 设为 false", e);
            }
        } else {
            this.configManager = null;
            log.info("Nacos 未启用（nacos.enabled=false），使用 classpath 静态技能（降级模式）");
        }

        this.chatClient = buildChatClient();
    }

    /**
     * 构建 ChatClient——支持 Nacos 动态模式与 classpath 降级模式。
     */
    private ChatClient buildChatClient() {
        ToolCallback skillsTool;

        if (nacosEnabled && configManager != null) {
            // Nacos 模式：从配置中心读取 SKILL.md 内容，动态构建 SkillsTool
            SkillsTool.Builder builder = SkillsTool.builder();
            for (String dataId : NacosSkillConfigManager.SKILL_DATA_IDS) {
                String content = configManager.getSkillContent(dataId);
                if (content != null && !content.isEmpty()) {
                    builder.addSkillsResource(
                            new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8), dataId + ".md"));
                }
            }
            skillsTool = builder.build();
            log.info("SkillsTool 已从 Nacos 加载（{} 个技能）", NacosSkillConfigManager.SKILL_DATA_IDS.size());
        } else {
            // Classpath 降级模式——与 demo14 行为一致
            skillsTool = SkillsTool.builder()
                    .addSkillsResource(new ClassPathResource("skills/kb-reader/SKILL.md"))
                    .addSkillsResource(new ClassPathResource("skills/script-runner/SKILL.md"))
                    .build();
            log.info("SkillsTool 已从 classpath 加载（降级模式）");
        }

        return chatClientBuilder
                .defaultToolCallbacks(skillsTool)
                .defaultTools(FileSystemTools.builder().allowedDirectory(".").build())
                .defaultTools(ShellTools.builder().workingDirectory(".").build())
                .defaultSystem(s -> s.text(
                        "你是'小北优选咖啡店'的智能助手，技能指令由 Nacos 配置中心动态管理。\n" +
                        "你拥有 kb-reader（门店知识库）与 script-runner（脚本执行）两项技能。\n" +
                        "技能描述可能随时热更新，请始终按当前加载的指令操作；不要编造未读到的内容。"))
                .build();
    }

    /**
     * 配置变更后线程安全地重建 ChatClient。
     */
    private synchronized void rebuildChatClient() {
        log.info("检测到 Nacos 配置变更，正在重建 ChatClient...");
        this.chatClient = buildChatClient();
        log.info("ChatClient 重建完成，新技能定义已生效");
    }

    /**
     * GET /api/demo15/ask?question=会员积分怎么算？
     * GET /api/demo15/ask?question=运行脚本统计知识库
     */
    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "帮我查看会员积分规则") String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * GET /api/demo15/skills —— 查看当前加载的技能清单与来源。
     */
    @GetMapping("/skills")
    public Map<String, Object> skills() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nacosEnabled", nacosEnabled);

        if (nacosEnabled && configManager != null) {
            result.put("mode", "nacos");
            Map<String, String> all = configManager.getAllSkills();
            result.put("count", all.size());
            result.put("skills", all.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> skill = new LinkedHashMap<>();
                        skill.put("dataId", e.getKey());
                        skill.put("contentLength", e.getValue() != null ? e.getValue().length() : 0);
                        skill.put("content", e.getValue());
                        return skill;
                    })
                    .toList());
        } else {
            result.put("mode", "classpath（降级）");
            result.put("count", 2);
            result.put("skills", java.util.List.of("kb-reader", "script-runner"));
        }

        return result;
    }

    /**
     * POST /api/demo15/refresh —— 手动触发技能重载（教学用途，无需等 Nacos 推送）。
     */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        if (!nacosEnabled) {
            return Map.of("status", "skipped",
                    "message", "Nacos 未启用，技能由 classpath 加载，无需刷新");
        }
        rebuildChatClient();
        return Map.of("status", "ok",
                "message", "技能已从 Nacos 重新加载，当前版本已生效");
    }
}
