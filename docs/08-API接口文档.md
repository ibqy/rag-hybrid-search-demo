# 八、API 接口文档

## RAG 问答接口

> 源文件：[`RagController.java`](../src/main/java/com/xb/rag/controller/RagController.java)

```bash
POST /api/rag/ask
Content-Type: application/json

{
  "question": "系统登录步骤是什么？",
  "topK": 8,
  "vectorTopK": 20,
  "bm25TopK": 20,
  "enableHallucinationCheck": true,
  "tenantId": "tenant_001"
}
```

### 响应示例

```json
{
  "question": "系统登录步骤是什么？",
  "answer": "系统登录步骤如下[1]：\n1. 打开浏览器访问公司门户\n2. 输入用户名和密码\n3. 点击「登录」按钮\n\n[1] 来源: 用户操作手册.pdf, 页码: 3",
  "citations": [
    {
      "id": 1,
      "citationStr": "[1] (来源: 用户操作手册.pdf, 页码: 3)\n打开浏览器输入URL，输入用户名密码，点击登录"
    }
  ],
  "hasHallucination": 0,
  "costMs": 2340
}
```

### 参数说明

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| question | string | 必填 | 用户问题 |
| topK | int | 8 | 最终返回的片段数 |
| vectorTopK | int | 20 | 向量检索召回数 |
| bm25TopK | int | 20 | BM25 检索召回数 |
| enableHallucinationCheck | bool | false | 是否启用幻觉检测 |
| tenantId | string | - | 租户隔离过滤 |

## 文档管理接口

> 源文件：[`DocController.java`](../src/main/java/com/xb/rag/controller/DocController.java)

### 上传文档

```bash
POST /api/docs/upload
Content-Type: multipart/form-data

file: @用户手册.pdf
tenantId: tenant_001
```

响应：
```json
{
  "docId": "uuid-xxx",
  "docName": "用户手册.pdf",
  "chunkCount": 23,
  "md5": "a1b2c3d4e5f6..."
}
```

### 增量更新文档

```bash
POST /api/docs/update
Content-Type: multipart/form-data

file: @用户手册-v2.pdf
docId: uuid-xxx
tenantId: tenant_001
```

响应：
```json
{
  "docId": "uuid-xxx",
  "status": "updated",
  "chunks": 25,
  "md5": "f6e5d4c3b2a1..."
}
```

## 评估接口

> 源文件：[`EvalController.java`](../src/main/java/com/xb/rag/controller/EvalController.java)

```bash
POST /api/eval/run
Content-Type: application/json

{
  "samples": [
    {
      "question": "系统登录步骤",
      "relevantChunkIds": ["chunk_001", "chunk_002"],
      "expectedAnswer": "打开浏览器..."
    }
  ],
  "topK": 8
}
```

## 完整调用链路示例

```
                      ┌─────────────────────────────┐
                      │   POST /api/docs/upload      │
                      │   上传文档 → 解析 → 写入向量库│
                      └──────────┬──────────────────┘
                                 ▼
                      ┌─────────────────────────────┐
                      │   POST /api/rag/ask          │
                      │   提问 → 混合检索 → Rerank   │
                      │   → 组装上下文 → LLM回答     │
                      │   → 幻觉校验 → 返回引用结果  │
                      └─────────────────────────────┘
```