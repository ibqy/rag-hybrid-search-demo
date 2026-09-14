package com.xb.rag.evaluation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 评估报告 —— 包含所有指标和样本级明细
 */
public class EvalReport {

    private final List<EvalMetrics> metrics;
    private final List<EvalDataset.EvalSample> samples;
    private final int hallucinationCount;
    private final String generatedAt;

    public EvalReport(List<EvalMetrics> metrics, List<EvalDataset.EvalSample> samples, int hallucinationCount) {
        this.metrics = metrics != null ? List.copyOf(metrics) : List.of();
        this.samples = samples != null ? List.copyOf(samples) : List.of();
        this.hallucinationCount = hallucinationCount;
        this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public List<EvalMetrics> metrics() { return metrics; }
    public List<EvalDataset.EvalSample> samples() { return samples; }
    public int hallucinationCount() { return hallucinationCount; }
    public String generatedAt() { return generatedAt; }

    /**
     * 输出易读的指标报表
     */
    public String toReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAG 评估报告 ===\n");
        sb.append("生成时间: ").append(generatedAt).append("\n");
        sb.append("测试样本数: ").append(samples.size()).append("\n\n");
        for (EvalMetrics m : metrics) {
            sb.append("  ").append(m).append("\n");
        }
        sb.append("\n幻觉样本数: ").append(hallucinationCount).append("\n");
        return sb.toString();
    }
}