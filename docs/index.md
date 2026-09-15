---
layout: home

hero:
  name: RAG 高阶混合检索知识库
  text: 生产级高阶 RAG 教学项目
  tagline: 从零实现完整高阶 RAG 链路 —— 文档预处理 → 高级分块 → 多路混合检索 → Rerank 精排 → 上下文组装 → 幻觉抑制 → 向量库运维 → 增量更新 → 评估体系
  actions:
    - theme: brand
      text: 开始学习 →
      link: /01-文档预处理
    - theme: alt
      text: GitHub 源码
      link: https://github.com/ibqy/rag-hybrid-search-demo

features:
  - icon: 📄
    title: 文档预处理
    details: PDF / Word / Markdown 解析、OCR 识别、文本清洗、元数据抽取
  - icon: ✂️
    title: 高级分块策略
    details: 层级 / 语义 / 固定 / 自适应重叠四种分块方案对比
  - icon: 🔀
    title: 多路混合检索
    details: Vector + BM25 双路召回，RRF 融合排序
  - icon: 🎯
    title: Rerank 与上下文组装
    details: 精排、阈值过滤、去重、引用构建
  - icon: 🛡️
    title: 幻觉抑制
    details: 约束 Prompt + 引用验证 + 自校验三环抑制机制
  - icon: 📊
    title: RAG 评估体系
    details: Recall@K / Precision@K / 幻觉率量化评估
  - icon: 🗄️
    title: 向量库运维
    details: 索引选型、分区管理、MD5 增量同步、软删除
  - icon: 🧰
    title: 技术栈
    details: Java 21 · Spring Boot 3.4 · WebFlux · Spring AI · Milvus · Elasticsearch
---
