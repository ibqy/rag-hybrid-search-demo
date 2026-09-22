package com.xb.rag.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 评估指标 —— 封装 Recall@K / Precision@K / MRR@K / nDCG@K 等量化结果
 *
 * @author ibqy
 */
public class EvalMetrics {

    private final String metricName;
    private final double value;
    private final int totalQueries;
    private final int correctCount;

    public EvalMetrics(
            @JsonProperty("metricName") String metricName,
            @JsonProperty("value") double value,
            @JsonProperty("totalQueries") int totalQueries,
            @JsonProperty("correctCount") int correctCount) {
        this.metricName = metricName;
        this.value = value;
        this.totalQueries = totalQueries;
        this.correctCount = correctCount;
    }

    @JsonProperty("metricName")
    public String metricName() { return metricName; }

    @JsonProperty("value")
    public double value() { return value; }

    @JsonProperty("totalQueries")
    public int totalQueries() { return totalQueries; }

    @JsonProperty("correctCount")
    public int correctCount() { return correctCount; }

    @Override
    public String toString() {
        return String.format("%s: %.4f (%d/%d)", metricName, value, correctCount, totalQueries);
    }
}
