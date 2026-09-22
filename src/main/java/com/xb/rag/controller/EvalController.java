package com.xb.rag.controller;

import com.xb.rag.evaluation.EvalDataset;
import com.xb.rag.evaluation.EvalReport;
import com.xb.rag.evaluation.EvalRunner;
import com.xb.rag.evaluation.EvalMetrics;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * RAG 评估接口 —— 启动评估、查看指标
 *
 * POST /api/eval/run  运行评估
 *
 * @author ibqy
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
        validate(request);
        return Mono.fromCallable(() -> {
            EvalDataset dataset = new EvalDataset();
            for (EvalSampleJson s : request.samples()) {
                dataset.addSample(new EvalDataset.EvalSample(
                        s.question(), s.relevantChunkIds(), s.expectedAnswer()));
            }
            return evalRunner.evaluate(dataset, request.topK());
        });
    }

    private void validate(EvalRequest request) {
        if (request == null || request.samples() == null || request.samples().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "samples 不能为空");
        }
        if (request.topK() < 1 || request.topK() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "topK 必须在 1~100 之间");
        }
        for (EvalSampleJson s : request.samples()) {
            if (s == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sample 条目不能为 null");
            }
            if (s.question() == null || s.question().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question 不能为空");
            }
            if (s.relevantChunkIds() == null || s.relevantChunkIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "relevantChunkIds 不能为空");
            }
            for (String id : s.relevantChunkIds()) {
                if (id == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "relevantChunkIds 中不能包含 null");
                }
            }
        }
    }

    public record EvalRequest(List<EvalSampleJson> samples, int topK) {}
    public record EvalSampleJson(String question, List<String> relevantChunkIds, String expectedAnswer) {}
}