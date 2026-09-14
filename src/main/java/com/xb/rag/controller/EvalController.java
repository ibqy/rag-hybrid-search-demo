package com.xb.rag.controller;

import com.xb.rag.evaluation.EvalDataset;
import com.xb.rag.evaluation.EvalReport;
import com.xb.rag.evaluation.EvalRunner;
import com.xb.rag.evaluation.EvalMetrics;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * RAG 评估接口 —— 启动评估、查看指标
 *
 * POST /api/eval/run  运行评估
 */
@RestController
@RequestMapping("/api/eval")
public class EvalController {

    private final EvalRunner evalRunner;

    public EvalController(EvalRunner evalRunner) {
        this.evalRunner = evalRunner;
    }

    /**
     * 运行评估
     *
     * @param request 评估请求（测试集 + topK 参数）
     * @return 评估报告
     */
    @PostMapping("/run")
    public Mono<EvalReport> run(@RequestBody EvalRequest request) {
        return Mono.fromCallable(() -> {
            EvalDataset dataset = new EvalDataset();
            for (EvalSampleJson s : request.samples()) {
                dataset.addSample(new EvalDataset.EvalSample(
                        s.question(), s.relevantChunkIds(), s.expectedAnswer()));
            }
            EvalReport report = evalRunner.evaluate(dataset, request.topK());
            return report;
        });
    }

    public record EvalRequest(List<EvalSampleJson> samples, int topK) {}
    public record EvalSampleJson(String question, List<String> relevantChunkIds, String expectedAnswer) {}
}