package com.xb.springai.controller.demo15;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * Nacos 技能配置管理器——把 Agent Skills 的 SKILL.md 从 classpath 迁移到 Nacos 配置中心。
 *
 * <p>核心能力：</p>
 * <ol>
 *     <li><b>配置热更新</b>：通过 Nacos Listener 监听配置变更，零重启更新技能指令</li>
 *     <li><b>版本回滚</b>：Nacos 原生保留配置历史版本，在控制台一键回滚</li>
 *     <li><b>降级策略</b>：若 Nacos 不可用，controller 层自动回退 classpath 静态技能</li>
 * </ol>
 *
 * <p>Nacos 配置项约定（GROUP = SKILL_GROUP）：</p>
 * <ul>
 *     <li>dataId = skill-kb-reader —— 门店知识库阅读技能</li>
 *     <li>dataId = skill-script-runner —— 脚本执行技能</li>
 * </ul>
 *
 * <p>作者：ibqy | 日期：2026-09-15</p>
 */
public class NacosSkillConfigManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NacosSkillConfigManager.class);

    /** Nacos 分组名 */
    public static final String SKILL_GROUP = "SKILL_GROUP";

    /** 预置的两个技能 dataId */
    public static final List<String> SKILL_DATA_IDS = List.of("skill-kb-reader", "skill-script-runner");

    private final ConfigService configService;

    /** 技能缓存：dataId → SKILL.md 内容 */
    private final Map<String, String> skillContents = new ConcurrentHashMap<>();

    /** 配置变更回调列表（每个回调触发一次 ChatClient 重建） */
    private final List<Runnable> changeCallbacks = new CopyOnWriteArrayList<>();

    /**
     * @param serverAddr  Nacos 服务地址，如 127.0.0.1:8848
     * @throws NacosException  连接失败时抛出
     */
    public NacosSkillConfigManager(String serverAddr) throws NacosException {
        Properties props = new Properties();
        props.setProperty("serverAddr", serverAddr);
        // 不设 namespace 则用 public；不设 username/password 则无鉴权（教学环境）
        this.configService = NacosFactory.createConfigService(props);

        for (String dataId : SKILL_DATA_IDS) {
            loadAndListen(dataId);
        }
        log.info("NacosSkillConfigManager 初始化完成，serverAddr={}，已加载 {} 个技能",
                serverAddr, SKILL_DATA_IDS.size());
    }

    /**
     * 首次加载配置 + 注册监听器。
     */
    private void loadAndListen(String dataId) throws NacosException {
        // 1) 首次拉取配置
        String content = configService.getConfig(dataId, SKILL_GROUP, 5000);
        if (content != null && !content.isEmpty()) {
            skillContents.put(dataId, content);
            log.info("技能加载成功: dataId={}，内容长度={}", dataId, content.length());
        } else {
            log.warn("技能配置为空: dataId={}，请通过 Nacos 控制台或 nacos-init.sh 推送 SKILL.md", dataId);
        }

        // 2) 注册监听器——Nacos 配置变更时自动回调
        configService.addListener(dataId, SKILL_GROUP, new Listener() {
            @Override
            public Executor getExecutor() {
                return null; // 使用 Nacos 内置线程池
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("配置变更通知: dataId={}，新内容长度={}",
                        dataId, configInfo != null ? configInfo.length() : 0);
                skillContents.put(dataId, configInfo);
                // 通知所有观察者：技能已变更，请重建 ChatClient
                changeCallbacks.forEach(cb -> {
                    try {
                        cb.run();
                    } catch (Exception e) {
                        log.error("变更回调执行失败: dataId={}", dataId, e);
                    }
                });
            }
        });
    }

    /** 获取某个技能的 SKILL.md 内容 */
    public String getSkillContent(String dataId) {
        return skillContents.get(dataId);
    }

    /** 获取全部技能的 dataId → SKILL.md 内容映射 */
    public Map<String, String> getAllSkills() {
        return new HashMap<>(skillContents);
    }

    /** 注册变更回调（配置变更后触发） */
    public void onChange(Runnable callback) {
        changeCallbacks.add(callback);
    }

    @Override
    public void close() {
        // ConfigService 会自行管理长轮询连接
    }
}
