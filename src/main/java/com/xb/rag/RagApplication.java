package com.xb.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * RAG 高阶优化教学项目 入口
 *
 * 全链路：文档预处理 → 高级分块 → 混合检索 → Rerank → 幻觉抑制 → 向量库运维 → 增量更新
 *
 * @author ibqy
 */
@SpringBootApplication
@EnableKafka
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}