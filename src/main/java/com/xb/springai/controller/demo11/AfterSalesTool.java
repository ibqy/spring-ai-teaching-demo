package com.xb.springai.controller.demo11;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * demo11 用到的工具：查询售后政策
 *
 * <p>作者：ibqy | 日期：2026-09-10</p>
 *
 * <p>真实客服系统里，售后政策往往存储在数据库/配置中心。这里用内存常量化身为
 * "售后知识"，让模型在用户追问时能实时调用。用 {@link Tool} 注解的方法会自动
 * 暴露给模型，作为函数调用的一部分。</p>
 *
 * @author ibqy
 */
@Component
public class AfterSalesTool {

    /**
     * 查询指定品类的售后政策。
     * @param category 商品品类，如 咖啡豆 / 器具 / 周边
     * @return 该品类的退换货政策
     */
    @Tool(description = "查询指定商品品类的退换货与售后政策，当用户询问售后、退换、质量问题、保修时调用")
    public String getAfterSalePolicy(String category) {
        return switch (category) {
            case "咖啡豆" -> "咖啡豆属于食品，开封后不支持退换；未开封且7天内可退。";
            case "器具" -> "器具支持7天无理由退换，1年内非人为损坏可免费换新。";
            case "周边" -> "周边产品支持7天无理由退换，不影响二次销售即可。";
            default -> "请提供具体品类以便查询。";
        };
    }
}