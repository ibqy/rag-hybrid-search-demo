package com.xb.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 基础上下文加载测试 —— 验证 Spring 容器能正常启动
 */
@SpringBootTest
@ActiveProfiles("test")
class RagApplicationTests {

    @Test
    void contextLoads() {
        // 只要容器能启动就说明所有 Bean 注入正确
    }
}