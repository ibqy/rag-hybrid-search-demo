package com.xb.rag.evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估数据集 —— 构造测试问答对及其标准参考段落
 *
 * 每条记录包含：
 * - question: 用户问题
 * - relevantChunkIds: 该问题对应的正确参考 chunk ID 列表
 * - expectedAnswer: 期望的标准回答（用于判断回答忠实度）
 */
public class EvalDataset {

    private final List<EvalSample> samples;

    public EvalDataset() {
        this.samples = new ArrayList<>();
    }

    public EvalDataset(List<EvalSample> samples) {
        this.samples = new ArrayList<>(samples);
    }

    public void addSample(EvalSample sample) {
        this.samples.add(sample);
    }

    public List<EvalSample> getSamples() { return Collections.unmodifiableList(samples); }

    public int size() { return samples.size(); }

    /**
     * 单个测试样本
     */
    public static class EvalSample {
        private final String question;
        private final List<String> relevantChunkIds;
        private final String expectedAnswer;

        public EvalSample(String question, List<String> relevantChunkIds, String expectedAnswer) {
            this.question = question;
            this.relevantChunkIds = List.copyOf(relevantChunkIds);
            this.expectedAnswer = expectedAnswer;
        }

        public String question() { return question; }
        public List<String> relevantChunkIds() { return relevantChunkIds; }
        public String expectedAnswer() { return expectedAnswer; }
    }
}