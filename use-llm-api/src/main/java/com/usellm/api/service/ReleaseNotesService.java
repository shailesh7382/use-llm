package com.usellm.api.service;

import com.usellm.api.dto.CommitAnalysisDto;
import com.usellm.api.dto.ReleaseNotesRequestDto;
import com.usellm.api.dto.ReleaseNotesResponseDto;
import com.usellm.api.service.releasenotes.GitCommitData;
import com.usellm.api.service.releasenotes.GitCommitReader;
import com.usellm.client.config.LLMClientConfig;
import com.usellm.core.exception.LLMException;
import com.usellm.core.model.ChatRequest;
import com.usellm.core.model.ChatResponse;
import com.usellm.core.model.Message;
import com.usellm.core.port.LLMPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ReleaseNotesService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseNotesService.class);

    private final GitCommitReader gitCommitReader;
    private final LLMPort llmPort;
    private final LLMClientConfig llmClientConfig;

    public ReleaseNotesService(GitCommitReader gitCommitReader,
                               LLMPort llmPort,
                               LLMClientConfig llmClientConfig) {
        this.gitCommitReader = gitCommitReader;
        this.llmPort = llmPort;
        this.llmClientConfig = llmClientConfig;
    }

    public Mono<ReleaseNotesResponseDto> generate(ReleaseNotesRequestDto request) {
        String model = resolveModel(request);
        String effectiveBaseRef = gitCommitReader.resolveEffectiveBaseRef(request);
        List<GitCommitData> commits = gitCommitReader.readCommits(request);

        log.info("Generating release notes for repoPath={}, branch={}, baseRef={}, commits={}",
                request.getRepoPath(), request.getBranch(), effectiveBaseRef, commits.size());

        return Flux.fromIterable(commits)
                .concatMap(commit -> summarizeCommit(commit, model))
                .collectList()
                .flatMap(commitAnalyses -> buildReleaseNotes(request, effectiveBaseRef, model, commits, commitAnalyses));
    }

    private Mono<CommitAnalysisDto> summarizeCommit(GitCommitData commit, String model) {
        List<Message> messages = List.of(
                Message.system("""
                        You analyze one git commit at a time for release notes.
                        Read the commit message and patch carefully.
                        Produce a concise, accurate summary in 2-4 sentences.
                        Focus on user-visible impact first, then important implementation detail.
                        If the change is internal only, say that clearly.
                        Do not invent behavior that is not supported by the commit data.
                        """),
                Message.user(buildCommitPrompt(commit))
        );

        return llmPort.chat(ChatRequest.builder()
                        .model(model)
                        .messages(messages)
                        .maxTokens(300)
                        .temperature(0.2)
                        .stream(false)
                        .build())
                .map(this::extractContent)
                .map(summary -> CommitAnalysisDto.builder()
                        .sha(commit.getSha())
                        .shortSha(commit.getShortSha())
                        .author(commit.getAuthor())
                        .committedAt(commit.getCommittedAt())
                        .subject(commit.getSubject())
                        .summary(summary.isBlank() ? commit.getSubject() : summary)
                        .build());
    }

    private Mono<ReleaseNotesResponseDto> buildReleaseNotes(ReleaseNotesRequestDto request,
                                                            String effectiveBaseRef,
                                                            String model,
                                                            List<GitCommitData> commits,
                                                            List<CommitAnalysisDto> analyses) {
        List<Message> messages = List.of(
                Message.system("""
                        You write polished software release notes for engineering teams and end users.
                        Use the provided commit-by-commit analyses as the source of truth.
                        Group related changes together, remove repetition, and keep the wording natural.
                        Output markdown with:
                        1. a short title,
                        2. a brief overview paragraph,
                        3. a bullet list of notable changes.
                        Mention internal-only work only when it is relevant for maintainers.
                        """),
                Message.user(buildReleaseNotesPrompt(request, effectiveBaseRef, analyses))
        );

        return llmPort.chat(ChatRequest.builder()
                        .model(model)
                        .messages(messages)
                        .maxTokens(800)
                        .temperature(0.3)
                        .stream(false)
                        .build())
                .map(this::extractContent)
                .map(releaseNotes -> ReleaseNotesResponseDto.builder()
                        .repoPath(request.getRepoPath())
                        .branch(request.getBranch())
                        .baseRef(effectiveBaseRef)
                        .model(model)
                        .commitCount(commits.size())
                        .commits(analyses)
                        .releaseNotes(releaseNotes)
                        .build());
    }

    private String resolveModel(ReleaseNotesRequestDto request) {
        if (request.getModel() != null && !request.getModel().isBlank()) {
            return request.getModel().trim();
        }
        return llmClientConfig.getDefaultModel();
    }

    private String buildCommitPrompt(GitCommitData commit) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Commit SHA: ").append(commit.getSha()).append('\n');
        prompt.append("Author: ").append(commit.getAuthor()).append('\n');
        prompt.append("Committed At: ").append(commit.getCommittedAt()).append('\n');
        prompt.append("Subject: ").append(commit.getSubject()).append('\n');
        prompt.append("Body:\n").append(emptyIfBlank(commit.getBody())).append("\n\n");
        prompt.append("Diff and file stats:\n").append(emptyIfBlank(commit.getDiff()));
        return prompt.toString();
    }

    private String buildReleaseNotesPrompt(ReleaseNotesRequestDto request,
                                           String effectiveBaseRef,
                                           List<CommitAnalysisDto> analyses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Repository Path: ").append(request.getRepoPath()).append('\n');
        prompt.append("Branch: ").append(request.getBranch()).append('\n');
        prompt.append("Base Ref: ").append(effectiveBaseRef != null ? effectiveBaseRef : "(none)").append('\n');
        prompt.append("Commit Analyses:\n");
        for (int i = 0; i < analyses.size(); i++) {
            CommitAnalysisDto analysis = analyses.get(i);
            prompt.append(i + 1).append(". ")
                    .append(analysis.getShortSha()).append(" - ")
                    .append(analysis.getSubject()).append('\n')
                    .append("   Summary: ").append(analysis.getSummary()).append('\n');
        }
        return prompt.toString();
    }

    private String emptyIfBlank(String value) {
        return value == null || value.isBlank() ? "(not provided)" : value;
    }

    private String extractContent(ChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()
                && response.getChoices().get(0).getMessage() != null
                && response.getChoices().get(0).getMessage().getContent() != null) {
            return response.getChoices().get(0).getMessage().getContent().trim();
        }
        throw new LLMException("LLM returned an empty response while generating release notes", 502, "empty_llm_response");
    }
}
