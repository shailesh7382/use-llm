package com.usellm.api.service;

import com.usellm.api.dto.ReleaseNotesRequestDto;
import com.usellm.api.dto.ReleaseNotesResponseDto;
import com.usellm.api.service.releasenotes.GitCommitData;
import com.usellm.api.service.releasenotes.GitCommitReader;
import com.usellm.client.config.LLMClientConfig;
import com.usellm.core.model.ChatResponse;
import com.usellm.core.model.Message;
import com.usellm.core.port.LLMPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseNotesServiceTest {

    @Test
    void generateSummarizesCommitsOneByOneAndBuildsFinalNotes() {
        GitCommitReader gitCommitReader = new GitCommitReader() {
            @Override
            public List<GitCommitData> readCommits(ReleaseNotesRequestDto request) {
                return List.of(
                        GitCommitData.builder()
                                .sha("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                                .shortSha("aaaaaaa")
                                .author("Alice")
                                .committedAt("2026-05-10T10:15:30Z")
                                .subject("Add release notes endpoint")
                                .body("Introduces a new endpoint")
                                .diff("diff --git a/file b/file\n+new endpoint")
                                .build(),
                        GitCommitData.builder()
                                .sha("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                                .shortSha("bbbbbbb")
                                .author("Bob")
                                .committedAt("2026-05-10T11:15:30Z")
                                .subject("Refine prompts")
                                .body("Improves generated text")
                                .diff("diff --git a/file b/file\n+better prompt")
                                .build()
                );
            }

            @Override
            public String resolveEffectiveBaseRef(ReleaseNotesRequestDto request) {
                return "main";
            }
        };

        AtomicInteger chatCalls = new AtomicInteger();
        LLMPort llmPort = new LLMPort() {
            @Override
            public Mono<ChatResponse> chat(com.usellm.core.model.ChatRequest request) {
                int call = chatCalls.incrementAndGet();
                String content = switch (call) {
                    case 1 -> "Adds a new API endpoint for generating release notes from git history.";
                    case 2 -> "Improves prompt wording so the final notes read more naturally.";
                    default -> "## Release Notes\n\nThis update adds release-note generation and improves output quality.\n\n- Added an API endpoint for release notes.\n- Improved the prompt design for clearer summaries.";
                };
                return Mono.just(chatResponse(content));
            }

            @Override
            public Mono<com.usellm.core.model.ModelListResponse> listModels() { return Mono.empty(); }
            @Override
            public Mono<com.usellm.core.model.LLMModel> getModel(String modelId) { return Mono.empty(); }
            @Override
            public Mono<com.usellm.core.model.CompletionResponse> complete(com.usellm.core.model.CompletionRequest request) { return Mono.empty(); }
            @Override
            public Flux<com.usellm.core.model.CompletionResponse> streamComplete(com.usellm.core.model.CompletionRequest request) { return Flux.empty(); }
            @Override
            public Flux<ChatResponse> streamChat(com.usellm.core.model.ChatRequest request) { return Flux.empty(); }
        };

        LLMClientConfig config = new LLMClientConfig();
        config.setDefaultModel("test-model");

        ReleaseNotesService service = new ReleaseNotesService(gitCommitReader, llmPort, config);
        ReleaseNotesRequestDto request = ReleaseNotesRequestDto.builder()
                .repoPath("/tmp/repo")
                .branch("feature/release-notes")
                .build();

        StepVerifier.create(service.generate(request))
                .assertNext(response -> {
                    assertEquals(2, response.getCommitCount());
                    assertEquals("main", response.getBaseRef());
                    assertEquals("test-model", response.getModel());
                    assertEquals(2, response.getCommits().size());
                    assertTrue(response.getReleaseNotes().contains("Release Notes"));
                })
                .verifyComplete();

        assertEquals(3, chatCalls.get());
    }

    private ChatResponse chatResponse(String content) {
        ChatResponse.ChatChoice choice = ChatResponse.ChatChoice.builder()
                .index(0)
                .finishReason("stop")
                .message(Message.assistant(content))
                .build();
        return ChatResponse.builder()
                .id("chatcmpl-test")
                .created(Instant.now().getEpochSecond())
                .model("test-model")
                .choices(List.of(choice))
                .build();
    }
}
