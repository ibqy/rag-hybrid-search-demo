package com.xb.rag.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 评估报告 —— 包含所有指标和样本级明细
 *
 * @author ibqy
 */
public class EvalReport {

    private final List<EvalMetrics> metrics;
    private final List<EvalSampleView> samples;
    private final String generatedAt;

    public EvalReport(List<EvalMetrics> metrics, List<EvalDataset.EvalSample> samples, int hallucinationCount,
                      List<List<String>> retrievedIdsPerSample) {
        this.metrics = metrics != null ? List.copyOf(metrics) : List.of();
        this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<EvalSampleView> views;
        if (samples != null && retrievedIdsPerSample != null && samples.size() == retrievedIdsPerSample.size()) {
            views = new java.util.ArrayList<>();
            for (int i = 0; i < samples.size(); i++) {
                views.add(new EvalSampleView(samples.get(i), retrievedIdsPerSample.get(i)));
            }
            views = List.copyOf(views);
        } else if (samples != null) {
            views = samples.stream().map(s -> new EvalSampleView(s, List.of())).toList();
        } else {
            views = List.of();
        }
        this.samples = views;
    }

    @JsonProperty("metrics")
    public List<EvalMetrics> metrics() { return metrics; }

    @JsonProperty("samples")
    public List<EvalSampleView> samples() { return samples; }

    @JsonProperty("generatedAt")
    public String generatedAt() { return generatedAt; }

    @JsonIgnore
    public int hallucinationCount() { return 0; }

    /**
     * 输出易读的指标报表
     */
    @JsonIgnore
    public String toReportString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RAG 评估报告 ===\n");
        sb.append("生成时间: ").append(generatedAt).append("\n");
        sb.append("测试样本数: ").append(samples.size()).append("\n\n");
        for (EvalMetrics m : metrics) {
            sb.append("  ").append(m).append("\n");
        }
        return sb.toString();
    }

    public static class EvalSampleView {
        private final String question;
        private final List<String> relevantChunkIds;
        private final List<String> retrievedChunkIds;

        public EvalSampleView(EvalDataset.EvalSample sample, List<String> retrievedChunkIds) {
            this.question = sample.question();
            this.relevantChunkIds = sample.relevantChunkIds();
            this.retrievedChunkIds = retrievedChunkIds != null ? List.copyOf(retrievedChunkIds) : List.of();
        }

        @JsonProperty("question")
        public String question() { return question; }

        @JsonProperty("relevantChunkIds")
        public List<String> relevantChunkIds() { return relevantChunkIds; }

        @JsonProperty("retrievedChunkIds")
        public List<String> retrievedChunkIds() { return retrievedChunkIds; }
    }
}
