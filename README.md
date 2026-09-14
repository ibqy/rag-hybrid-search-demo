# RAG 高阶混合检索知识库 - 教学项目

解决生产 RAG 三大痛点：**召回不准、幻觉严重、文档处理能力弱**

## 项目概览

从零实现一套完整的高阶 RAG 链路：文档预处理 → 高级分块 → 多路混合检索 → Rerank 精排 → 上下文组装 → 幻觉抑制 → 向量库运维 → 增量更新 → 评估体系

## 技术栈

Java 21 + Spring Boot 3.4.4 + WebFlux + Spring AI + Milvus + Elasticsearch + PDFBox + Tess4J

## 架构分层

```
┌──────────────────────────────────────────┐
│          Controller (API 入口)             │
│  RagController / DocController / EvalCtrl │
├──────────────────────────────────────────┤
│          Hallucination (幻觉抑制)           │
│  HallucinationDetector / CitationValidator│
├──────────────────────────────────────────┤
│  Context (上下文组装)  │ Rerank (精排过滤)  │
│  ContextBuilder       │ RerankService     │
│                       │ ResultFilter      │
├──────────────────────────────────────────┤
│          Retrieval (混合检索)              │
│  VectorRetriever + Bm25Retriever + RRF   │
├──────────────────────────────────────────┤
│          Chunking (高级分块)               │
│  层级 / 语义 / 固定 / 自适应重叠           │
├──────────────────────────────────────────┤
│          Document (文档预处理)             │
│  PDFBox / docx4j / OCR / 清洗 / 元数据    │
├──────────────────────────────────────────┤
│     VectorStore (向量库运维 + 增量更新)    │
│  pgvector / Milvus / MD5 / 软删除         │
└──────────────────────────────────────────┘
```

## 教学文档

| 章节 | 内容 | 对应源码 |
|------|------|---------|
| [01-文档预处理](docs/01-文档预处理.md) | PDF/Word/Markdown 解析、OCR、清洗、元数据 | [document/](../src/main/java/com/xb/rag/document/) |
| [02-高级分块策略](docs/02-高级分块策略.md) | 层级/语义/自适应四种分块方案对比 | [chunking/](../src/main/java/com/xb/rag/chunking/) |
| [03-多路混合检索](docs/03-多路混合检索.md) | Vector + BM25 + RRF 融合 | [retrieval/](../src/main/java/com/xb/rag/retrieval/) |
| [04-Rerank与上下文组装](docs/04-Rerank与上下文组装.md) | 精排、阈值过滤、去重、引用构建 | [rerank/](../src/main/java/com/xb/rag/rerank/) + [context/](../src/main/java/com/xb/rag/context/) |
| [05-幻觉抑制](docs/05-幻觉抑制.md) | 三环抑制：约束Prompt+引用验证+自校验 | [hallucination/](../src/main/java/com/xb/rag/hallucination/) |
| [06-向量库运维与增量更新](docs/06-向量库运维与增量更新.md) | 索引选型、分区、MD5增量同步 | [vectorstore/](../src/main/java/com/xb/rag/vectorstore/) |
| [07-RAG评估体系](docs/07-RAG评估体系.md) | Recall@K / Precision@K / 幻觉率 | [evaluation/](../src/main/java/com/xb/rag/evaluation/) |
| [08-API接口文档](docs/08-API接口文档.md) | 接口定义、参数说明、调用链路 | [controller/](../src/main/java/com/xb/rag/controller/) |

## 提交历史（学习路线）

```
1.  chore: 项目脚手架搭建
2.  feat: 文档预处理模块
3.  feat: 高级分块策略
4.  feat: 多路混合检索 + RRF 融合
5.  feat: Rerank重排 + 上下文组装
6.  feat: 幻觉抑制 + 溯源引用机制
7.  feat: 向量库运维 + 增量更新
8.  feat: RAG 评估体系
9.  feat: REST API 控制器入口
10. docs: 教学文档
```

每个 commit 对应一个独立模块，建议按顺序阅读学习。

## 快速开始

```bash
# 1. 配置环境变量
export OPENAI_API_KEY=sk-xxx
export OPENAI_BASE_URL=https://api.openai.com

# 2. 启动依赖服务（Docker Compose）
docker compose up -d

# 3. 启动应用
./mvnw spring-boot:run

# 4. 上传文档并提问
curl -X POST http://localhost:8080/api/docs/upload \
  -F "file=@doc.pdf" -F "tenantId=test"

curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "文档内容是什么？"}'
```

## 配置说明

详见 [application.yml](src/main/resources/application.yml)，核心参数：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `retrieval.vector-top-k` | 20 | 向量召回数 |
| `retrieval.bm25-top-k` | 20 | BM25 召回数 |
| `retrieval.final-top-k` | 8 | 最终返回数 |
| `retrieval.score-threshold` | 0.45 | Rerank 过滤阈值 |
| `retrieval.rerank-url` | - | Rerank 服务地址 |
| `vectorstore.type` | pgvector | 向量库选型 |