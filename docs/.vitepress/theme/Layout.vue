<script setup>
import DefaultTheme from 'vitepress/theme'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-features-before>
      <section class="sig">
        <div class="sig-card">
          <span class="sig-label">PIPELINE · MIXED RETRIEVAL</span>
          <h3 class="sig-title">两路召回，一路精排</h3>
          <div class="rf-flow">
            <span class="sg-node">文档预处理</span>
            <span class="sg-arr">→</span>
            <span class="sg-node">高级分块</span>
            <span class="sg-arr">→</span>
            <span class="sg-node">Embedding</span>
            <span class="sg-arr">→</span>
            <div class="rf-branch">
              <span class="sg-node">BM25 召回</span>
              <span class="sg-node">向量召回</span>
            </div>
            <span class="sg-arr">→</span>
            <span class="sg-fill">RRF 融合</span>
            <span class="sg-arr">→</span>
            <span class="sg-fill">Rerank 精排</span>
            <span class="sg-arr">→</span>
            <span class="sg-node">上下文组装</span>
            <span class="sg-arr">→</span>
            <span class="sg-line">生成 · 幻觉抑制</span>
          </div>
          <p class="sig-note">检索质量决定生成质量。图里的每一步，都对应仓库中一章可运行的实现——从文档预处理到 RAG 评估体系，整条链路自己写一遍。</p>
        </div>
      </section>
    </template>
    <template #home-features-after>
      <div class="sig-more"><a href="https://ibqy.github.io/" target="_blank" rel="noopener">← ibqy.github.io · 探索全部项目</a></div>
    </template>
  </Layout>
</template>
