# 七、RAG 评估体系

## 问题背景

绝大多数 RAG 项目上线前没有量化评估，"凭感觉"说效果好不好。生产必须用指标衡量召回质量、幻觉率。

## 评估指标

> 源文件：[`EvalMetrics.java`](../src/main/java/com/xb/rag/evaluation/EvalMetrics.java)

### Recall@K（召回率）
公式：`正确召回的相关 chunk 数 / 总相关 chunk 数`
- 衡量检索阶段是否把正确资料找回来
- Recall@5：前 5 条结果中有多少比例的正确答案

### Precision@K（精确率）
公式：`正确召回的相关 chunk 数 / 返回的 chunk 总数`
- 衡量检索结果的精确度
- 越低说明噪音越多

### 幻觉率（HallucinationRate）
公式：`存在幻觉的回答数 / 总回答数`
- 调用 `HallucinationDetector` 逐条检查
- 生产目标 < 5%

## 测试数据集

> 源文件：[`EvalDataset.java`](../src/main/java/com/xb/rag/evaluation/EvalDataset.java)

每条测试记录包含：
```java
public record EvalSample(
    String question,           // 用户问题
    List<String> relevantChunkIds,  // 正确答案对应的 chunk ID
    String expectedAnswer      // 期望的标准回答
) {}
```

构造示例：
```
Q: "系统登录步骤是什么？"
  → relevant: ["chunk_001", "chunk_002"]
  → expected: "打开浏览器输入URL → 输入用户名密码 → 点击登录"

Q: "ORD-2024-001 的状态"
  → relevant: ["chunk_015"]
  → expected: "订单 ORD-2024-001 已发货"
```

## 评估执行器

> 源文件：[`EvalRunner.java`](../src/main/java/com/xb/rag/evaluation/EvalRunner.java)

```java
public EvalReport evaluate(EvalDataset dataset, int topK) {
    for (EvalSample sample : dataset.getSamples()) {
        // 1. 执行检索
        List<SearchResult> results = retriever.apply(sample.question());
        
        // 2. 计算 Recall@K
        long hits = relevant.stream().filter(retrievedIds::contains).count();
        
        // 3. 检查幻觉
        HallucinationCheckResult check = detector.check(...);
    }
    return report;
}
```

## 评估报告

> 源文件：[`EvalReport.java`](../src/main/java/com/xb/rag/evaluation/EvalReport.java)

```
=== RAG 评估报告 ===
生成时间: 2026-09-14T20:30:00
测试样本数: 100

  Recall@8: 0.8734 (87/100)
  Precision@8: 0.6520 (65/100)
  HallucinationRate: 0.0300 (3/100)
```

## API 调用

> 源文件：[`EvalController.java`](../src/main/java/com/xb/rag/controller/EvalController.java)

```bash
POST /api/eval/run
{
  "samples": [
    {"question": "...", "relevantChunkIds": ["..."], "expectedAnswer": "..."}
  ],
  "topK": 8
}
```

## 如何用评估驱动优化

1. 基线评估：先用固定切分 + 向量检索跑一遍，记录 Recall@K
2. 优化分块策略：改为层级切片，Recall@K 是否有提升？
3. 加 BM25 混合检索：对比融合前后的 Recall@K
4. 加 Rerank：Precision@K 是否提升？幻觉率是否下降？
5. 每次修改只改一个变量，A/B 对比

## 面试要点

- Recall@K 低怎么办？检查分块策略是否破坏了语义，增大 topK，加 BM25 混合检索
- 幻觉率高怎么办？强化 System Prompt，启用自校验，检查 Rerank 阈值是否太低
- 评估数据怎么来？从真实用户问题中采样，人工标注正确 chunk