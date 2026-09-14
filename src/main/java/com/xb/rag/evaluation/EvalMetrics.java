package com.xb.rag.evaluation;

import java.util.Collections;
import java.util.List;

/**
 * 评估指标 —— 封装 Recall@K / Precision@K / 幻觉率等量化结果
 */
public class EvalMetrics {

    private final String metricName;
    private final double value;
    private final int totalQueries;
    private final int correctCount;

    public EvalMetrics(String metricName, double value, int totalQueries, int correctCount) {
        this.metricName = metricName;
        this.value = value;
        this.totalQueries = totalQueries;
        this.correctCount = correctCount;
    }

    public String metricName() { return metricName; }
    public double value() { return value; }
    public int totalQueries() { return totalQueries; }
    public int correctCount() { return correctCount; }

    @Override
    public String toString() {
        return String.format("%s: %.4f (%d/%d)", metricName, value, correctCount, totalQueries);
    }
}