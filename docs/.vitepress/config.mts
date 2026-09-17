import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'RAG 高阶混合检索知识库',
  description: 'Java RAG 教学与评测实验：两路召回、RRF 融合、冻结排名消融、逐查询指标，以及文档处理与真实服务集成边界。',
  base: '/rag-hybrid-search-demo/',
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
          tokens[idx].attrSet('href', `https://github.com/ibqy/rag-hybrid-search-demo/${kind}/main/${rel}`)
        }
        return defaultLink(tokens, idx, options, env, self)
      }
    }
  },
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: '评测实验室', link: '/07-RAG评估体系' },
      { text: '学习路线', link: '/#learning-path' },
      { text: 'API 与边界', link: '/08-API接口文档' }
    ],
    sidebar: [
      {
        text: '证据与检索',
        items: [
          { text: '01 · 文档预处理', link: '/01-文档预处理' },
          { text: '02 · 分块策略与标签版本', link: '/02-高级分块策略' },
          { text: '03 · 两路召回与 RRF', link: '/03-多路混合检索' }
        ]
      },
      {
        text: '组装与集成边界',
        items: [
          { text: '04 · Rerank 与上下文', link: '/04-Rerank与上下文组装' },
          { text: '05 · 回答校验的边界', link: '/05-幻觉抑制' },
          { text: '06 · 增量更新与运维骨架', link: '/06-向量库运维与增量更新' }
        ]
      },
      {
        text: '实验与验证',
        items: [
          { text: '07 · 评测实验室', link: '/07-RAG评估体系' },
          { text: '08 · API 与集成边界', link: '/08-API接口文档' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ibqy/rag-hybrid-search-demo' }
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
      message: '教学与实验项目 · 合成排名不等于业务基准 · 服务集成边界见文档',
      copyright: 'Copyright © 2026 ibqy'
    }
  }
})
