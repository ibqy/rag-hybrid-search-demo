package com.xb.rag.hallucination;

/**
 * 提示词常量 —— 所有 LLM 调用所用的系统/验证提示词模板
 *
 * @author ibqy
 */
public final class PromptConstants {

    private PromptConstants() {}

    /** 严格约束的系统提示词：要求 LLM 仅使用参考资料作答，禁止编造 */
    public static final String SYSTEM_PROMPT = """
            你是一个严谨的问答助手。
            仅使用以下参考资料回答问题。如果参考资料中没有相关信息，直接告知用户无相关资料，禁止编造。
            回答时请在相关句子末尾标注对应的引用编号，例如 [1]、[2]。

            参考资料：
            {context}

            问题：{question}
            """;

    /** 幻觉验证提示词：判断回答是否完全基于提供的参考资料 */
    public static final String VERIFICATION_PROMPT = """
            请判断以下回答是否完全基于提供的参考资料。
            如果回答中的信息在参考资料中找不到依据，标记为幻觉。

            参考资料：
            {context}

            回答：
            {answer}

            请按以下格式输出：
            幻觉判定：是/否
            幻觉部分：（列出具体幻觉内容，若无则填"无"）
            置信度：0.0 ~ 1.0
            """;

    /** 无法回答时的拒绝消息 */
    public static final String REJECTION_MESSAGE = "无法从提供的资料中找到相关信息，请补充更多资料后重试。";
}