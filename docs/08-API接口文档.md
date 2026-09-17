# 八、API 接口与集成边界

## 调用前必读

这些是控制器的请求形状与逻辑说明，**不是所有端点已经完成真实服务联调的承诺**。先运行 [第 07 章的离线测试](./07-RAG评估体系.md)，再准备模型、向量库、Elasticsearch 及已写入的两路索引。

| 接口 | 控制器逻辑 | 不能据此推断 |
|---|---|---|
| `POST /api/rag/ask` | 混合检索 → 空结果拒答 / 上下文 → 模型回答 → 可选自检 → 引用校验 | 未调用 Rerank；未实施租户过滤 |
| `POST /api/docs/upload` | 读取文件、构建元数据、解析、分块，返回统计 | 不写入向量库或 Elasticsearch；上传成功不代表可检索 |
| `POST /api/docs/update` | 解析新文件并调用更新骨架 | `updated` 不是持久化成功或双索引一致性的证明 |
| `POST /api/eval/run` | 用提交的标签集运行实际混合检索排名评测 | 不生成答案、不计算幻觉率；需已有索引 |

文档控制器没有实际的 `DELETE /api/docs/{docId}` 映射，不要把源码注释里的设想当作可调用 API。Multipart 参数适配、服务自动装配及错误响应仍需在选定 Web 运行栈下联调。

## 问答接口

源码：[RagController.java](../src/main/java/com/xb/rag/controller/RagController.java)。以下在依赖已就绪、两路索引已有同版本 chunk 后使用：

```bash
curl -X POST http://localhost:8080/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"系统登录步骤是什么？","topK":8,"vectorTopK":20,"bm25TopK":20,"enableHallucinationCheck":false}'
```

| 参数 | 类型 | 当前行为 |
|---|---|---|
| `question` | string | 必须非空白 |
| `topK` | int | 正值为融合输出上限；未给或非正时使用默认值，配置为 8 |
| `vectorTopK` | int | 正值为向量候选数；未给或非正时控制器使用 20 |
| `bm25TopK` | int | 正值为 BM25 候选数；未给或非正时控制器使用 20 |
| `enableHallucinationCheck` | boolean | 默认 false；true 额外调用模型检查回答 |
| `tenantId` | string | 字段存在，但控制器未用于检索过滤；**不是安全边界** |

`retrieval.score-threshold` 不在当前问答控制器中执行过滤；空列表会拒答，但非空不等于证据充分。RRF 分数不可套用 Rerank 的 0.45 阈值。

### 响应字段

不预填虚构答案或耗时；响应由实际服务调用决定。

| 字段 | 含义 |
|---|---|
| `question` / `answer` | 原始问题与经过引用校验的回答，或空结果拒答文本 |
| `citations` | 引用列表，每项包含 `id`、`result` 和 `citationStr`；结果元数据来自检索 |
| `hasHallucination` | 自检标记 0/1；未启用检测时也为 0，**不能解释为已验证无幻觉** |
| `costMs` | 当前请求记录的耗时，不是基准测试结果 |

引用编号有效只说明能关联到所给片段，不能证明片段支持答案中的每项事实。当前响应也不是完整的模型自检审计记录。

## 文档上传：解析预览，不是导入

源码：[DocController.java](../src/main/java/com/xb/rag/controller/DocController.java)。

```bash
curl -X POST http://localhost:8080/api/docs/upload \
  -F "file=@用户手册.pdf" \
  -F "tenantId=demo"
```

Multipart 字段：`file` 必填；`tenantId` 默认 `default`，用于文档元数据。响应字段为 `docId`、`docName`、`chunkCount`、`md5`。

`chunkCount` 是本次解析分块数量，不是已入库数量；`docId` 也不保证存在持久化文档记录。应检查解析是否成功、片段是否为空、元数据是否完整，不能仅以 HTTP 成功判断文档可搜索。

## 文档更新：占位写入边界

```bash
curl -X POST http://localhost:8080/api/docs/update \
  -F "file=@用户手册-v2.pdf" \
  -F "docId=替换为文档ID" \
  -F "tenantId=demo"
```

响应含 `docId`、`status`、`chunks`、`md5`。其中 `status: updated` 表示控制器已走到返回分支；`IncrementalUpdater` 的写入与底层管理操作仍是骨架，不能据此确认旧 chunk 已删除、新 chunk 已入库，或历史指纹已持久化。

需要实现的双索引一致性与幂等验收见 [第 06 章](./06-向量库运维与增量更新.md)。

## 排名评测接口

源码：[EvalController.java](../src/main/java/com/xb/rag/controller/EvalController.java)。

以下是**请求形状示例**。`chunk_001` / `chunk_002` 必须替换成当前冻结索引中人工标注的真实 ID；仓库没有预装这些业务数据。

```bash
curl -X POST http://localhost:8080/api/eval/run \
  -H "Content-Type: application/json" \
  -d '{"samples":[{"question":"系统登录步骤是什么？","relevantChunkIds":["chunk_001","chunk_002"],"expectedAnswer":"打开入口并完成身份验证"}],"topK":8}'
```

- `samples` 不能为空；问题必须有效，相关 ID 集合必须有标签。没有标签不是“无答案”。
- `topK` 必须为有效正整数；与问答接口的默认回退语义不同。
- `expectedAnswer` 保留在样本中，但本评测不生成或评分答案。
- `EvalRunner` 通过 `HybridSearchService` 查询真实候选，不是将每个问题固定当空结果。
- 报告给出 Recall@K、固定分母 Precision@K、MRR@K、二值 nDCG@K 的宏平均及逐查询 trace；具体序列化字段以 `EvalReport` 为准。
- 输入校验拒绝与 HTTP 错误状态/响应体的映射是两层契约，客户端应以当前控制器测试及联调结果为准，不假定统一错误 schema。

无需外部服务的入口是测试中的冻结排名消融，而不是这个 HTTP 接口；指标定义和完整实验步骤见 [第 07 章](./07-RAG评估体系.md)。

## 真实调用链与尚未连接的位置

```text
上传 → 解析 → 分块 → 返回统计
                   └─ 待实现：embedding + 向量/文本双索引写入

已有两路索引 → HybridSearchService → RRF → 上下文 → LLM → 引用校验
                                            └─ 可选：回答自检
              待接入：Rerank、权限过滤、完整上下文预算控制

已有两路索引 + 人工标签 → EvalRunner → 排名指标 + 逐查询 trace
```

## 联调验收清单

1. 确认向量存储 Bean、模型凭据与 embedding 维度匹配；ES 实际索引、字段和分析器匹配。
2. 对同一已知 chunk 做两路查询，确认 ID、版本与内容一致，而不是只检查端口可达。
3. 验证“有标签但返回空”的请求保留 0 分；空数据集、空标签与非法 K 被拒绝。
4. 核对问答中实际引用的结果；未启用自检的 0 标记不得包装为质量保证。
5. 不在隔离能力缺失时承载多个租户的敏感数据；请求中的 `tenantId` 不可信，真正授权必须来自认证上下文并约束两路检索。
