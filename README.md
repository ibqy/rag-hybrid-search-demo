# RAG 高阶混合检索知识库

**专有名词靠关键词，语义改写靠向量；融合是否有效，用逐查询评测回答。**

这是 Java / Spring AI 教学仓库，不是开箱即用的生产知识库。学习重点是把两路候选、RRF 排序与检索评测连起来，理解文档处理、重排和回答校验的工程边界。

[评测实验室](docs/07-RAG评估体系.md) · [学习路线](#学习路线) · [API 与集成边界](docs/08-API接口文档.md)

## 先分清：算法实验、服务集成、占位实现

| 范围 | 当前能力 | 不代表什么 |
|---|---|---|
| RRF 与排名评测 | 两路排名融合；Recall@K、Precision@K、MRR@K、二值 nDCG@K；逐查询结果 | 不评估生成答案，也不证明 RRF 总是优于单路 |
| 离线消融 | 冻结的合成 Vector / BM25 排名快照，与 RRF 对照 | 不调用真实 embedding / 索引，不是线上效果或性能基准 |
| 实际检索与问答 | `HybridSearchService` 连接向量库与 Elasticsearch；问答控制器组装上下文并调用模型 | 需要外部服务、兼容配置和已有索引；不是已验证的全链路部署 |
| 文档处理 | 解析、清洗、分块的教学实现 | `/api/docs/upload` 返回解析结果，不写入可搜索索引 |
| Rerank | 外部逐 query-text 对 HTTP 客户端 | `RagController` 尚未调用；需自行部署兼容服务 |
| 增量更新与运维 | 指纹比较、管理接口骨架 | 新增/更新写入及索引、分区、删除操作仍为日志占位 |
| 租户字段 | 请求与元数据中有 `tenantId` | 问答链路未使用它过滤，没有租户隔离 |

## 第一次运行：先做离线算法实验

在仓库根目录执行，要求 **Java 21、Maven 3.9+**：

```bash
mvn -Dtest=RrfFusionTest,EvalRunnerTest,RetrievalAblationTest test
```

这组定向测试用于检查融合规则、指标口径和冻结排名消融，不要求运行数据库或调用模型。首次构建仍需下载 Maven 依赖；仓库没有 Maven Wrapper，请勿使用 `./mvnw`。

实验输出：`target/evaluation/ablation.json`。先看每条 query 的两路排名、融合排名与相关性标签，再比较宏平均指标。**报告里的分数只属于该合成 fixture，不是业务实测结果。** 命令、手算示例及实验设计见 [第 07 章](docs/07-RAG评估体系.md)。

不要把上述定向测试与全量 `mvn test` 混为一谈：应用上下文测试还需要外部服务和配置。

## 学习路线

不依赖提交历史，按问题选择入口：

1. **先复现，再解释**：07 的离线实验 → 03 的 RRF 手算 → 回到 07 检查单条回归。
2. **先查证据，再调模型**：01 检查解析结果 → 02 比较分块 → 03 检查候选集是否含答案。
3. **准备接入真实服务**：08 核对 API 边界 → 04 理解重排分数与预算 → 05 区分引用与事实支持 → 06 规划一致性验收。

| 章节 | 你将解决的问题 | 源码入口 |
|---|---|---|
| [01 · 文档预处理](docs/01-文档预处理.md) | 证据是否被解析丢失、清洗误删？ | [document/](src/main/java/com/xb/rag/document/) |
| [02 · 高级分块策略](docs/02-高级分块策略.md) | 如何控制边界、重叠和标签版本？ | [chunking/](src/main/java/com/xb/rag/chunking/) |
| [03 · 多路混合检索](docs/03-多路混合检索.md) | 为什么累加排名贡献，而不是混加原始分数？ | [retrieval/](src/main/java/com/xb/rag/retrieval/) |
| [04 · Rerank 与上下文](docs/04-Rerank与上下文组装.md) | 候选数、精排数与上下文预算如何分开？ | [rerank/](src/main/java/com/xb/rag/rerank/) / [context/](src/main/java/com/xb/rag/context/) |
| [05 · 幻觉抑制](docs/05-幻觉抑制.md) | 引用有效为什么不等于答案正确？ | [hallucination/](src/main/java/com/xb/rag/hallucination/) |
| [06 · 增量更新与运维](docs/06-向量库运维与增量更新.md) | 如何验收双索引写入、删除与权限一致性？ | [vectorstore/](src/main/java/com/xb/rag/vectorstore/) |
| [07 · 评测实验室](docs/07-RAG评估体系.md) | 如何做无泄漏、可复现的消融和逐查询复盘？ | [evaluation/](src/main/java/com/xb/rag/evaluation/) |
| [08 · API 与边界](docs/08-API接口文档.md) | 哪些接口有逻辑，哪些依赖尚待接通？ | [controller/](src/main/java/com/xb/rag/controller/) |

## 可选：真实服务集成

技术栈包含 Java 21、Spring Boot、WebFlux、Spring AI、Elasticsearch、向量存储适配以及 PDFBox / docx4j / Tess4J。具体依赖版本以 [pom.xml](pom.xml) 为准；不要按“最新版”文档盲目替换 API。

1. 阅读 [application.yml](src/main/resources/application.yml) 与 [docker-compose.yml](docker-compose.yml)。Compose 提供基础设施，不提供模型、Rerank 服务或已导入数据；默认不启动 Milvus profile。
2. 配置模型凭据、兼容的向量存储 Bean / 连接参数，核对 embedding 维度与索引 schema。Compose 启动成功不等于 Spring AI 自动装配完成。
3. 用独立的、已验证的导入方式准备两路索引，确保相同 `chunkId` 对应相同文本和版本。**不要把上传接口当成导入器。**
4. 再启动应用并按 [第 08 章](docs/08-API接口文档.md) 联调。以下仅是启动命令，不是全链路通过声明：

```bash
# Git Bash / POSIX shell；凭据从本地环境注入，勿提交到仓库
export OPENAI_API_KEY="你的实际凭据"
export OPENAI_BASE_URL="https://api.openai.com"
docker compose up -d
mvn spring-boot:run
```

开发用 Compose 含默认口令及未开启认证的 Elasticsearch，不应直接暴露到公网。

### 关键配置及语义

| 配置 | 配置文件值 | 注意事项 |
|---|---|---|
| `retrieval.vector-top-k` / `retrieval.bm25-top-k` | 20 / 20 | 两路候选预算，不是最终评测 K |
| `retrieval.final-top-k` | 8 | 默认融合输出上限；实验应显式固定 K |
| `retrieval.score-threshold` | 0.45 | Rerank 客户端使用的阈值，不是 RRF 分数阈值；问答控制器未应用它 |
| `retrieval.rerank-url` | `http://localhost:8000/rerank` | 需要兼容的外部逐对打分服务，未接入问答链路 |
| `elasticsearch.index` | `rag_docs` | 导入与检索必须指向同一索引，不能只确认端口连通 |
| `vectorstore.type` | `pgvector` | 选型字段不等于已有完整存储实现或自动建表 |

核对 `HybridSearchService` 的 `retrieval.*` 绑定与索引名称后，再判断调参是否实际生效。

## 文档站

已有 Node.js / npm 环境后，在仓库根目录运行：

```bash
npm ci
npm run docs:dev
# 静态构建检查
npm run docs:build
```

## 官方参考

- [Elasticsearch RRF](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/reciprocal-rank-fusion)：排名融合与候选窗口。
- [Elasticsearch ranking evaluation](https://www.elastic.co/docs/reference/elasticsearch/rest-apis/search-rank-eval)：标注查询集与指标解释；各实现默认口径需单独核对。
- [Spring AI RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)：检索增强组件与集成模式，不表示本仓库已接入全部能力。
