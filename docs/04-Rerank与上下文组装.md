# 四、Rerank 重排 + 结果过滤 + 上下文组装

## 问题背景

多路召回 20 条候选，里面仍有低相关片段。向量检索是"粗排"（找相似的），Rerank 是"精排"（判断是否相关）。这是 RAG 效果提升的核心手段。

## Rerank 重排

> 源文件：[`RerankService.java`](../src/main/java/com/xb/rag/rerank/RerankService.java)

Rerank 与向量检索的核心区别：
- 向量检索：query 和 chunk 各自编码为向量，余弦距离 ≈ 语义相似度
- Rerank：query + chunk 拼接输入交叉编码器，输出精确相关性分数

```java
// 逐条调用 rerank API
for (SearchResult candidate : candidates) {
    double score = callRerankApi(query, candidate.getContent());
    candidate.setScore(score); // 覆盖原有分数
}
```

### 降级策略

Rerank API 不可用时（网络超时、服务挂掉）：
```java
catch (WebClientRequestException e) {
    log.warn("Rerank 服务不可用，降级为原始排序");
    return fallbackSort(candidates, topK);
}
```
不阻塞主流程，保证系统可用性。

## 结果过滤

> 源文件：[`ResultFilter.java`](../src/main/java/com/xb/rag/rerank/ResultFilter.java)

三层过滤：

| 过滤层 | 方法 | 策略 |
|--------|------|------|
| 阈值过滤 | `filterByThreshold()` | 低于 0.45 直接丢弃 |
| 内容去重 | `deduplicate()` | 文本重叠 > 90% 合并保留高分 |
| Token预算 | `limitTokenCount()` | 超 LLM 上下文窗口时丢弃低分项 |

去重算法：滑动窗口采样，防止 O(n²)：
```java
int step = Math.max(1, shorter.length() / 100);
for (int i = 0; i <= shorter.length() - step; i += step) {
    String sub = shorter.substring(i, Math.min(i + step, shorter.length()));
    if (longer.contains(sub)) matchLen += sub.length();
}
```

## 上下文组装

> 源文件：[`ContextBuilder.java`](../src/main/java/com/xb/rag/context/ContextBuilder.java)

### 引用格式

```
[1] (来源: 用户手册.pdf, 页码: 12)
系统登录流程：打开浏览器输入 URL...

[2] (来源: API文档.md, 页码: 5)
POST /api/login 请求参数...
```

### 核心方法

```java
// 带元数据的上下文
public static String buildContextWithMeta(List<SearchResult> results)

// 自动截断超长上下文
public static String truncateIfExceeds(String context, int maxTokens)

// 构建可追溯的引用列表
public static List<SearchResultCitation> buildCitations(List<SearchResult> results)
```

截断策略：从最低分项开始丢弃，至少保留一条。

## 完整链路数据流

```
多路召回(20条) → Rerank精排 → 阈值过滤 → 去重 → Token截断 → 
上下文组装(8条带引用) → Prompt拼接 → LLM回答
```

## 面试要点

- Rerank 为什么比向量检索准？交叉编码器同时看到 query 和 chunk，双向注意力计算相关性
- 为什么 `scoreThreshold` 不能设太高？高阈值召回少，可能遗漏正确答案；0.45 是经验值
- 引用标记有什么用？LLM 回答时标注 `[1][2]`，用户可追溯到原文，减少幻觉