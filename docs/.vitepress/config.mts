import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'RAG 高阶混合检索知识库',
  description: '生产级 RAG 教学项目：文档预处理、高级分块、多路混合检索、Rerank 精排、幻觉抑制、评估体系',
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
      { text: 'GitHub', link: 'https://github.com/ibqy/rag-hybrid-search-demo' }
    ],
    sidebar: [
      {
        text: '教学文档',
        items: [
          { text: '01 · 文档预处理', link: '/01-文档预处理' },
          { text: '02 · 高级分块策略', link: '/02-高级分块策略' },
          { text: '03 · 多路混合检索', link: '/03-多路混合检索' },
          { text: '04 · Rerank 与上下文组装', link: '/04-Rerank与上下文组装' },
          { text: '05 · 幻觉抑制', link: '/05-幻觉抑制' },
          { text: '06 · 向量库运维与增量更新', link: '/06-向量库运维与增量更新' },
          { text: '07 · RAG 评估体系', link: '/07-RAG评估体系' },
          { text: '08 · API 接口文档', link: '/08-API接口文档' }
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
      message: '个人教学项目 · 代码可跑 · 注释记录设计取舍',
      copyright: 'Copyright © 2026 ibqy'
    }
  }
})
