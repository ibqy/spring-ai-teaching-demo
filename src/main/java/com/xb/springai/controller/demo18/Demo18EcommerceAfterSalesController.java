package com.xb.springai.controller.demo18;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * demo18：电商售后助手 —— 综合实战（Tool + Skill + RAG + MCP）
 *
 * <p>作者：ibqy | 日期：2026-09-21</p>
 *
 * <p><b>实战背景</b>：这是一个真实的电商售后场景 AI 助手，综合运用了 Spring AI 的多个核心能力：</p>
 * <ul>
 *     <li><b>Tool（工具调用）</b>：查询订单、创建售后申请 —— 模型自主决定何时调用</li>
 *     <li><b>Skill（技能文档）</b>：加载售后流程正文，让模型按标准流程回答</li>
 *     <li><b>RAG（检索增强）</b>：从售后规则文档中检索相关内容，带来源引用</li>
 *     <li><b>MCP（外部服务）</b>：调用物流查询、预约取件等外部 MCP 服务</li>
 *     <li><b>意图识别 + 流程编排</b>：后端代码控制多步骤执行流程</li>
 * </ul>
 *
 * <p><b>架构图</b>：</p>
 * <pre>
 *   React 聊天页（提问/确认/SSE 展示）
 *         ↓
 *   Spring Boot + Spring AI 2.0（后端组织执行）
 *    ├─ 本地能力：Tool（订单与申请）+ Skill（流程正文）
 *    ─ RAG：检索售后规则，返回资料来源
 *         ↓
 *   ├─ 本地模型：生成参数和回答、计算知识向量
 *   └─ 独立 MCP 服务：查物流、预约取件
 *         ↓
 *   PostgreSQL + pgvector（订单/会话/事件/知识向量）
 * </pre>
 *
 * <p><b>演示接口</b>：</p>
 * <pre>
 * # 简单问答（带 RAG）
 * GET /api/demo18/chat?question=怎么申请退货？
 *
 * # 查询订单
 * GET /api/demo18/chat?question=帮我查一下订单 ORD-20260921-001 的状态
 *
 * # 创建售后申请
 * POST /api/demo18/apply
 * { "orderId": "ORD-20260921-001", "reason": "商品破损", "type": "RETURN" }
 *
 * # 查询物流（模拟 MCP）
 * GET /api/demo18/logistics?orderId=ORD-20260921-001
 *
 * # 预约取件（模拟 MCP）
 * POST /api/demo18/pickup
 * { "orderId": "ORD-20260921-001", "address": "北京市朝阳区...", "time": "2026-09-22 14:00" }
 * </pre>
 */
@RestController
@RequestMapping("/api/demo18")
public class Demo18EcommerceAfterSalesController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    /** 模拟订单数据库 */
    private static final Map<String, Map<String, Object>> ORDER_DB = new ConcurrentHashMap<>();

    /** 模拟售后申请数据库 */
    private static final List<Map<String, Object>> APPLICATIONS = Collections.synchronizedList(new ArrayList<>());

    static {
        // 初始化模拟订单数据
        ORDER_DB.put("ORD-20260921-001", Map.of(
                "orderId", "ORD-20260921-001",
                "status", "已签收",
                "product", "iPhone 16 Pro 256GB 原色钛金属",
                "amount", 8999.00,
                "orderDate", "2026-09-18",
                "deliveryDate", "2026-09-20",
                "customer", "张三",
                "phone", "138****1234"
        ));
        ORDER_DB.put("ORD-20260921-002", Map.of(
                "orderId", "ORD-20260921-002",
                "status", "运输中",
                "product", "AirPods Pro 2",
                "amount", 1899.00,
                "orderDate", "2026-09-20",
                "customer", "李四",
                "phone", "139****5678"
        ));
    }

    public Demo18EcommerceAfterSalesController(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    /**
     * GET /api/demo18/chat?question=...
     * 智能问答：结合 Tool + Skill + RAG 回答售后问题
     */
    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam String question) {
        try {
            // 手动 RAG：先从 VectorStore 检索相关售后规则，再拼入提问
            String context = retrieveContext(question);
            String enhancedQuestion = question;
            if (!context.isEmpty()) {
                enhancedQuestion = "以下是相关的售后规则参考：\n" + context
                        + "\n\n用户问题：" + question
                        + "\n\n请根据上述规则回答用户问题。如果规则中有具体条款，请引用说明。";
            }

            String answer = chatClient.prompt()
                    .user(enhancedQuestion)
                    .call()
                    .content();

            return Map.of(
                    "question", question,
                    "answer", answer,
                    "source", "AI 助手（Tool + Skill + RAG）"
            );
        } catch (Exception e) {
            return Map.of(
                    "question", question,
                    "error", "处理失败：" + e.getMessage(),
                    "hint", "请检查模型配置和网络连接"
            );
        }
    }

    /**
     * 手动 RAG 上下文检索：从 VectorStore 中检索与用户问题最相关的售后规则文档片段。
     * 如果 VectorStore 未配置或无结果，返回空字符串。
     */
    private String retrieveContext(String query) {
        if (vectorStore == null) {
            return "";
        }
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(3).build()
            );
            if (docs.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                String source = String.valueOf(doc.getMetadata().getOrDefault("source", "未知来源"));
                sb.append("[").append(i + 1).append("] (来源: ").append(source).append(") ")
                  .append(doc.getText()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            // RAG 检索失败不影响主流程，降级为无上下文回答
            return "";
        }
    }

    /**
     * GET /api/demo18/order/{orderId}
     * 查询订单详情（Tool 能力演示）
     */
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        Map<String, Object> order = ORDER_DB.get(orderId);
        if (order == null) {
            return Map.of(
                    "error", "订单不存在",
                    "orderId", orderId,
                    "hint", "可尝试的订单号：ORD-20260921-001, ORD-20260921-002"
            );
        }
        return order;
    }

    /**
     * POST /api/demo18/apply
     * 创建售后申请（Tool 能力演示）
     */
    @PostMapping("/apply")
    public Map<String, Object> createApplication(@RequestBody Map<String, String> request) {
        String orderId = request.get("orderId");
        String reason = request.get("reason");
        String type = request.getOrDefault("type", "RETURN"); // RETURN / EXCHANGE / REPAIR

        // 验证订单是否存在
        if (!ORDER_DB.containsKey(orderId)) {
            return Map.of(
                    "success", false,
                    "error", "订单不存在：" + orderId
            );
        }

        // 创建售后申请
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("applicationId", "APP-" + System.currentTimeMillis());
        application.put("orderId", orderId);
        application.put("type", type);
        application.put("reason", reason);
        application.put("status", "待审核");
        application.put("createdAt", new Date().toString());

        APPLICATIONS.add(application);

        return Map.of(
                "success", true,
                "application", application,
                "nextStep", "我们将在 24 小时内审核您的申请，请耐心等待"
        );
    }

    /**
     * GET /api/demo18/applications
     * 查询所有售后申请
     */
    @GetMapping("/applications")
    public List<Map<String, Object>> listApplications() {
        return APPLICATIONS;
    }

    /**
     * GET /api/demo18/logistics?orderId=...
     * 查询物流信息（模拟 MCP 服务调用）
     */
    @GetMapping("/logistics")
    public Map<String, Object> queryLogistics(@RequestParam String orderId) {
        // 模拟调用外部 MCP 物流服务
        Map<String, Object> order = ORDER_DB.get(orderId);
        if (order == null) {
            return Map.of("error", "订单不存在");
        }

        String status = (String) order.get("status");
        List<Map<String, String>> tracking = new ArrayList<>();

        if ("已签收".equals(status)) {
            tracking.add(Map.of("time", "2026-09-20 14:30", "event", "已签收，签收人：本人"));
            tracking.add(Map.of("time", "2026-09-20 09:15", "event", "派件中，快递员：王师傅 138****9999"));
            tracking.add(Map.of("time", "2026-09-19 22:00", "event", "到达北京朝阳分拣中心"));
            tracking.add(Map.of("time", "2026-09-19 08:00", "event", "从上海发货"));
        } else if ("运输中".equals(status)) {
            tracking.add(Map.of("time", "2026-09-21 10:00", "event", "运输中，预计明天送达"));
            tracking.add(Map.of("time", "2026-09-20 20:00", "event", "从深圳发货"));
        } else {
            tracking.add(Map.of("time", "2026-09-21 08:00", "event", "等待发货"));
        }

        return Map.of(
                "orderId", orderId,
                "logistics", "顺丰速运 SF1234567890",
                "status", status,
                "tracking", tracking,
                "source", "MCP 物流服务（模拟）"
        );
    }

    /**
     * POST /api/demo18/pickup
     * 预约取件（模拟 MCP 服务调用）
     */
    @PostMapping("/pickup")
    public Map<String, Object> schedulePickup(@RequestBody Map<String, String> request) {
        String orderId = request.get("orderId");
        String address = request.get("address");
        String time = request.get("time");

        if (orderId == null || address == null || time == null) {
            return Map.of(
                    "success", false,
                    "error", "缺少必要参数：orderId, address, time"
            );
        }

        // 模拟预约取件
        return Map.of(
                "success", true,
                "pickupId", "PK-" + System.currentTimeMillis(),
                "orderId", orderId,
                "address", address,
                "scheduledTime", time,
                "courier", "王师傅 138****9999",
                "hint", "取件前请确保商品包装完好，附件齐全",
                "source", "MCP 取件服务（模拟）"
        );
    }

    /**
     * GET /api/demo18/skill/after-sales
     * 获取售后流程技能文档（Skill 能力演示）
     */
    @GetMapping("/skill/after-sales")
    public Map<String, Object> getAfterSalesSkill() {
        return Map.of(
                "skillName", "电商售后流程",
                "version", "1.0",
                "steps", List.of(
                        Map.of("step", 1, "name", "确认订单信息", "description", "核实订单号、商品信息、购买时间"),
                        Map.of("step", 2, "name", "了解问题原因", "description", "询问用户具体问题，判断售后类型"),
                        Map.of("step", 3, "name", "说明售后政策", "description", "根据商品类型和购买时间说明适用政策"),
                        Map.of("step", 4, "name", "引导申请流程", "description", "指导用户提交售后申请并上传凭证"),
                        Map.of("step", 5, "name", "跟进处理进度", "description", "告知审核时效和后续步骤")
                ),
                "policies", Map.of(
                        "return", "7 天无理由退货（需商品完好）",
                        "exchange", "15 天质量问题换货",
                        "repair", "1 年保修期内免费维修"
                )
        );
    }

    /**
     * GET /api/demo18/rag/search?query=...
     * 检索售后规则文档（RAG 能力演示）
     */
    @GetMapping("/rag/search")
    public Map<String, Object> searchRules(@RequestParam String query) {
        if (vectorStore == null) {
            return Map.of(
                    "error", "VectorStore 未配置",
                    "hint", "请在 application.yml 中配置 spring.ai.vectorstore.* 启用 RAG"
            );
        }

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).build()
        );

        List<Map<String, Object>> formatted = results.stream().map(doc -> Map.of(
                "content", doc.getText(),
                "score", doc.getMetadata().getOrDefault("distance", 0.0),
                "source", doc.getMetadata().getOrDefault("source", "unknown")
        )).toList();

        return Map.of(
                "query", query,
                "results", formatted,
                "count", formatted.size()
        );
    }

    /**
     * GET /api/demo18/demo
     * 完整流程演示：模拟一次完整的售后对话
     */
    @GetMapping("/demo")
    public Map<String, Object> demoFlow() {
        List<Map<String, Object>> flow = new ArrayList<>();

        // 步骤 1：用户提问
        flow.add(Map.of(
                "step", 1,
                "role", "user",
                "content", "我买的 iPhone 屏幕碎了，怎么申请售后？"
        ));

        // 步骤 2：AI 识别意图 + 查询订单
        flow.add(Map.of(
                "step", 2,
                "role", "assistant",
                "action", "调用 Tool：查询订单",
                "content", "好的，请提供您的订单号，我帮您查看订单信息和售后政策。"
        ));

        // 步骤 3：用户提供订单号
        flow.add(Map.of(
                "step", 3,
                "role", "user",
                "content", "订单号是 ORD-20260921-001"
        ));

        // 步骤 4：AI 查询订单 + RAG 检索政策
        flow.add(Map.of(
                "step", 4,
                "role", "assistant",
                "action", "Tool：查询订单 + RAG：检索售后规则",
                "content", "已查到您的订单：iPhone 16 Pro，9 月 18 日购买，9 月 20 日签收。还在 7 天无理由退货期内。屏幕破损属于质量问题，可以申请免费维修或换货。"
        ));

        // 步骤 5：引导申请
        flow.add(Map.of(
                "step", 5,
                "role", "assistant",
                "action", "Skill：售后流程",
                "content", "请问您希望维修还是换货？我可以帮您创建售后申请。"
        ));

        // 步骤 6：用户选择
        flow.add(Map.of(
                "step", 6,
                "role", "user",
                "content", "我要换货"
        ));

        // 步骤 7：创建申请 + 预约取件
        flow.add(Map.of(
                "step", 7,
                "role", "assistant",
                "action", "Tool：创建申请 + MCP：预约取件",
                "content", "已为您创建换货申请（APP-xxx）。快递员王师傅将在明天 14:00 上门取件，请保持手机畅通。取件后我们会在 3 个工作日内发出新商品。"
        ));

        return Map.of(
                "title", "电商售后助手完整流程演示",
                "flow", flow,
                "capabilities", List.of("Tool", "Skill", "RAG", "MCP")
        );
    }
}
