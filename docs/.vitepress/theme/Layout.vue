<script setup>
import DefaultTheme from 'vitepress/theme'
import { withBase } from 'vitepress'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-features-before>
      <section class="sig" aria-labelledby="experiment-title">
        <div class="sig-card">
          <div class="sig-experiment">
            <span class="sig-label">RETRIEVAL LAB · SAME QUERIES, SAME K</span>
            <h2 id="experiment-title" class="sig-title">两路融合，同口径评测</h2>
            <p class="sig-intro">先看每条查询如何变化，再决定平均分是否值得相信。</p>
            <div class="rf-flow" aria-label="冻结的两路候选排名经 RRF 融合后进行排名评测">
              <div class="rf-branch">
                <span class="sg-node">Vector 排名快照</span>
                <span class="sg-node">BM25 排名快照</span>
              </div>
              <span class="sg-arr" aria-hidden="true">→</span>
              <span class="sg-fill">RRF 累加融合</span>
              <span class="sg-arr" aria-hidden="true">→</span>
              <span class="sg-line">排名指标 + 逐查询 trace</span>
            </div>
            <ul class="sg-chips" aria-label="采用相同标签和最终 K 的三个对照组">
              <li class="sg-chip">Vector-only</li>
              <li class="sg-chip">BM25-only</li>
              <li class="sg-chip">RRF</li>
            </ul>
            <p class="sig-note">冻结合成 fixture，不调用真实 embedding 或索引。结果用于检验算法行为，不是业务效果基准，也不证明 RRF 总是更好。</p>
            <a class="sig-link" :href="withBase('/07-RAG评估体系')">运行实验，阅读报告 <span aria-hidden="true">→</span></a>
          </div>
          <aside class="sig-status" aria-labelledby="scope-title">
            <h3 id="scope-title">实现边界，一眼看清</h3>
            <dl>
              <div>
                <dt><span class="scope-tag">算法实验</span> 融合与排名评测</dt>
                <dd>先用定向测试核对公式、指标与输入边界。</dd>
              </div>
              <div>
                <dt><span class="scope-tag scope-tag-neutral">外部依赖</span> 检索与模型调用</dt>
                <dd>需要实际服务、已有索引与兼容配置。</dd>
              </div>
              <div>
                <dt><span class="scope-tag scope-tag-neutral">尚待接通</span> 入库、精排与隔离</dt>
                <dd>上传不入库；问答未接 Rerank，也未按租户过滤。</dd>
              </div>
            </dl>
            <a class="sig-link" :href="withBase('/08-API接口文档')">核对接口边界 <span aria-hidden="true">→</span></a>
          </aside>
        </div>
      </section>
    </template>
    <template #home-features-after>
      <section id="learning-path" class="learning-path" aria-labelledby="learning-title">
        <div class="learning-heading">
          <span class="sig-label">LEARNING PATH</span>
          <h2 id="learning-title">按你现在的问题，选一条路线</h2>
          <p>无需顺着提交历史读，也不必一开始就启动所有服务。</p>
        </div>
        <ol class="learning-routes">
          <li>
            <span class="route-number" aria-hidden="true">01</span>
            <h3>我想先跑一个实验</h3>
            <p>离线消融 → RRF 手算 → 逐查询回归，先建立可解释的检索基线。</p>
            <a :href="withBase('/07-RAG评估体系')">从评测实验室开始 <span aria-hidden="true">→</span></a>
          </li>
          <li>
            <span class="route-number" aria-hidden="true">02</span>
            <h3>我的证据总是找不全</h3>
            <p>解析检查 → 分块边界 → 候选召回，先定位信息在哪一步丢失。</p>
            <a :href="withBase('/01-文档预处理')">从文档预处理开始 <span aria-hidden="true">→</span></a>
          </li>
          <li>
            <span class="route-number" aria-hidden="true">03</span>
            <h3>我准备连接真实服务</h3>
            <p>API 边界 → 重排与回答校验 → 双索引更新，把设计转成验收条件。</p>
            <a :href="withBase('/08-API接口文档')">从集成边界开始 <span aria-hidden="true">→</span></a>
          </li>
        </ol>
      </section>
      <div class="sig-more">
        <a href="https://github.com/ibqy/rag-hybrid-search-demo">查看 GitHub 源码</a>
        <span aria-hidden="true"> · </span>
        <a href="https://ibqy.github.io/">探索其他学习项目</a>
      </div>
    </template>
  </Layout>
</template>
