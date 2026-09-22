package com.xb.rag;

import com.xb.rag.controller.RagController;
import com.xb.rag.hallucination.HallucinationDetector;
import com.xb.rag.hallucination.PromptConstants;
import com.xb.rag.retrieval.HybridSearchService;
import com.xb.rag.retrieval.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RAG 控制器单元测试 —— 验证问答管道的检索→生成→引用验证流程
 */
class RagControllerTest {
    @Test
    void sendsQuestionAndEvidenceToModelOutsideEventLoop() {
        var hybrid = mock(HybridSearchService.class);
        var model = mock(ChatModel.class);
        var captured = new AtomicReference<Prompt>();
        var blockingThread = new AtomicReference<Boolean>();
        when(hybrid.search(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new SearchResult("chunk", "doc", "Refunds take five days.")));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            blockingThread.set(Schedulers.isInNonBlockingThread());
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Five days [1]"))));
        });
        var controller = new RagController(hybrid, model, mock(HallucinationDetector.class));
        var response = Mono.defer(() -> controller.ask(request())).subscribeOn(Schedulers.parallel()).block();

        assertThat(captured.get().getContents()).contains("When does the refund arrive?", "Refunds take five days.");
        assertThat(blockingThread.get()).isFalse();
        assertThat(response.answer()).isEqualTo("Five days [1]");
    }

    @Test
    void refusesOutOfRangeCitations() {
        var hybrid = mock(HybridSearchService.class);
        var model = mock(ChatModel.class);
        when(hybrid.search(anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(new SearchResult("chunk", "doc", "evidence")));
        when(model.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("answer [9]")))));
        var response = new RagController(hybrid, model, mock(HallucinationDetector.class)).ask(request()).block();
        assertThat(response.answer()).isEqualTo(PromptConstants.REJECTION_MESSAGE);
        assertThat(response.hasHallucination()).isEqualTo(1);
    }

    private RagController.RagRequest request() {
        return new RagController.RagRequest("When does the refund arrive?", 2, 2, 2, false, null);
    }
}
