package com.xb.rag.rerank;

import com.xb.rag.retrieval.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 重排序服务 —— 调用外部 BGE-Rerank HTTP 端点对候选结果重新打分排序
 *
 * @author ibqy
 */
@Service
public class RerankService {

    private static final Logger log = LoggerFactory.getLogger(RerankService.class);

    private final WebClient webClient;

    @Value("${retrieval.rerank-url}")
    private String rerankUrl;

    @Value("${retrieval.score-threshold:0.45}")
    private double scoreThreshold;

    public RerankService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 对候选结果执行重排序
     *
     * @param query     原始查询
     * @param candidates 候选结果列表
     * @param topK      返回前 topK 条
     * @return 重排序后的结果列表
     */
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        try {
            // 逐条调用 rerank API 获取相关性分数
            for (SearchResult candidate : candidates) {
                double score = callRerankApi(query, candidate.getContent());
                candidate.setScore(score);
            }

            // 按重排分数降序排列
            List<SearchResult> ranked = candidates.stream()
                    .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                    .toList();

            // 过滤低于阈值的低分结果
            List<SearchResult> filtered = ranked.stream()
                    .filter(r -> r.getScore() >= scoreThreshold)
                    .toList();

            // 截取 topK
            return filtered.size() > topK ? filtered.subList(0, topK) : filtered;

        } catch (WebClientRequestException | WebClientResponseException e) {
            // 降级策略：rerank 服务不可用时，按原始分数排序
            log.warn("Rerank API 不可用 ({}), 降级为按原始分数排序", e.getMessage());
            return fallbackSort(candidates, topK);
        }
    }

    /**
     * 调用外部 rerank API 获取单个文本与查询的相关性分数
     */
    private double callRerankApi(String query, String text) {
        Map<String, Object> requestBody = Map.of("query", query, "text", text);

        RerankResponse response = webClient.post()
                .uri(rerankUrl)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(RerankResponse.class)
                .block();

        if (response == null) {
            log.warn("Rerank API 返回空响应");
            return 0.0;
        }
        return response.score();
    }

    /**
     * 降级策略：直接按原始分数倒序排列并截取 topK
     */
    private List<SearchResult> fallbackSort(List<SearchResult> candidates, int topK) {
        List<SearchResult> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .toList();
        return sorted.size() > topK ? sorted.subList(0, topK) : sorted;
    }

    /**
     * Rerank API 响应结构
     */
    private record RerankResponse(double score, String text) {}
}