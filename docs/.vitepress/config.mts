import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'Spring AI 教学实战',
  description: 'Spring AI 2.0 渐进式教学：19 个 Demo 覆盖对话、RAG、工具调用、Agent Skills、MCP、多模态、A2A 智能体通信与综合实战',
  base: '/spring-ai-teaching-demo/',
  lastUpdated: true,
  markdown: {
    config(md) {
      const defaultLink =
        md.renderer.rules.link_open ||
        ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
      md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
        const href = tokens[idx].attrGet('href')
        if (href && href.startsWith('../')) {
          const rel = href.replace(/^(\.\.\/)+/, '')
          const kind = /\.[A-Za-z]+$/.test(rel) ? 'blob' : 'tree'
          tokens[idx].attrSet('href', `https://github.com/ibqy/spring-ai-teaching-demo/${kind}/main/${rel}`)
        }
        return defaultLink(tokens, idx, options, env, self)
      }
    }
  },
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: 'GitHub', link: 'https://github.com/ibqy/spring-ai-teaching-demo' }
    ],
    sidebar: [
      {
        text: '教学文档',
        items: [
          { text: '01 · 快速开始', link: '/01-quick-start' },
          { text: '02 · 核心概念', link: '/02-core-concepts' },
          { text: '03 · RAG 深入', link: '/03-rag-deep-dive' },
          { text: '04 · 工具调用', link: '/04-tool-calling-guide' },
          { text: '05 · Agent Skills', link: '/05-agent-skills-guide' },
          { text: '06 · ETL 管道', link: '/06-etl-pipeline-guide' },
          { text: '07 · Advisor', link: '/07-advisor-guide' },
          { text: '08 · 结构化输出', link: '/08-structured-output-guide' },
          { text: '09 · 生产检查清单', link: '/09-production-checklist' },
          { text: '10 · MCP', link: '/10-mcp-guide' },
          { text: '11 · 多模态', link: '/11-multimodal-guide' },
          { text: '12 · 电商售后实战', link: '/12-ecommerce-after-sales' },
          { text: '13 · A2A 通信', link: '/13-a2a-guide' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ibqy/spring-ai-teaching-demo' }
    ],
    search: { provider: 'local' },
    outline: { level: [2, 3], label: '本页目录' },
    docFooter: { prev: '上一篇', next: '下一篇' },
    lastUpdated: { text: '最后更新于' },
    darkModeSwitchLabel: '外观',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '文档',
    returnToTopLabel: '回到顶部',
    footer: {
      message: '个人教学项目 · 代码可跑 · 注释记录设计取舍',
      copyright: 'Copyright © 2026 ibqy'
    }
  }
})
