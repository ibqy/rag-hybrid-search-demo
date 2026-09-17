---
layout: home

hero:
  name: RAG 高阶混合检索
  text: 两路召回，效果用评测说话。
  tagline: 关键词找术语，向量找语义。用 RRF 融合排名，再以逐查询指标检查收益与退化。先跑不依赖外部服务的算法实验，再看真实检索、重排与入库尚需接通的边界。
  actions:
    - theme: brand
      text: 进入评测实验室
      link: /07-RAG评估体系
    - theme: alt
      text: 选择学习路线
      link: /#learning-path

features:
  - title: 01 / 证据先完整
    details: 解析组件 · 从 PDF 表格、Markdown 代码到清洗误删，检查证据是否真正进入片段；上传不等于入库。
    link: /01-文档预处理
    linkText: 检查文档处理边界
  - title: 02 / 分块要可比较
    details: 策略教学 · 比较固定、层级、语义分块；区分字符与 token、真实重叠与元数据，冻结 chunk 标签版本。
    link: /02-高级分块策略
    linkText: 设计分块对照
  - title: 03 / 融合不是混加分数
    details: 算法实验 · 手算 RRF 的跨路累加，理解 1-based 名次、重复项与稳定排序；候选 K 和最终 K 分开设置。
    link: /03-多路混合检索
    linkText: 从排名理解 RRF
  - title: 04 / 精排有接入成本
    details: 待集成 · Rerank 是外部逐对 HTTP 客户端，尚未进入问答链路；了解评分阈值、失败降级和引用预算。
    link: /04-Rerank与上下文组装
    linkText: 查看精排与上下文
  - title: 05 / 指标必须有证据
    details: 评测实验 · 对比冻结合成排名的单路与 RRF；学习 Recall、Precision、MRR、nDCG、分层标注与逐查询复盘。
    link: /07-RAG评估体系
    linkText: 运行排名消融
  - title: 06 / 知道哪里还没接通
    details: 集成边界 · 上传不写可搜索索引，增量运维仍有日志骨架，tenantId 不提供隔离；先明确验收条件再接服务。
    link: /08-API接口文档
    linkText: 核对 API 能力
---
