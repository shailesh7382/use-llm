package com.usellm.api.service.releasenotes;

import com.usellm.api.config.ReleaseNotesProperties;
import com.usellm.api.dto.ReleaseNotesRequestDto;
import com.usellm.core.exception.LLMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ShellGitCommitReader implements GitCommitReader {

    private static final Logger log = LoggerFactory.getLogger(ShellGitCommitReader.class);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(30);
    private static final String FIELD_SEPARATOR = "\u001f";
    private static final String EMPTY_GIT_FORMAT = "--format=";
    private final ReleaseNotesProperties releaseNotesProperties;

    public ShellGitCommitReader(ReleaseNotesProperties releaseNotesProperties) {
        this.releaseNotesProperties = releaseNotesProperties;
    }

    @Override
    public List<GitCommitData> readCommits(ReleaseNotesRequestDto request) {
        Path repoPath = validateRepoPath(request.getRepoPath());
        String branch = request.getBranch().trim();
        int maxCommits = normalizeMaxCommits(request.getMaxCommits());
        int maxDiffCharacters = normalizeMaxDiffCharacters(request.getMaxDiffCharacters());
        String effectiveBaseRef = resolveEffectiveBaseRef(request);

        verifyRefExists(repoPath, branch, "branch");
        if (effectiveBaseRef != null) {
            verifyRefExists(repoPath, effectiveBaseRef, "baseRef");
        }

        String range = effectiveBaseRef != null ? effectiveBaseRef + ".." + branch : branch;
        List<String> shas = readCommitShas(repoPath, range, maxCommits);
        if (shas.isEmpty()) {
            throw new LLMException("No commits found for the requested branch range", 400, "git_range_empty");
        }

        List<GitCommitData> commits = new ArrayList<>();
        for (String sha : shas) {
            commits.add(readCommit(repoPath, sha, maxDiffCharacters));
        }
        return commits;
    }

    @Override
    public String resolveEffectiveBaseRef(ReleaseNotesRequestDto request) {
        if (request.getBaseRef() != null && !request.getBaseRef().isBlank()) {
            return request.getBaseRef().trim();
        }

        Path repoPath = validateRepoPath(request.getRepoPath());
        String branch = request.getBranch().trim();
        String originHead = readOptional(repoPath, "symbolic-ref", "--quiet", "--short", "refs/remotes/origin/HEAD");
        if (originHead != null) {
            String normalized = originHead.trim();
            if (normalized.startsWith("origin/")) {
                normalized = normalized.substring("origin/".length());
            }
            if (!normalized.equals(branch)) {
                return normalized;
            }
        }

        if (!"main".equals(branch) && refExists(repoPath, "main")) {
            return "main";
        }
        if (!"master".equals(branch) && refExists(repoPath, "master")) {
            return "master";
        }
        return null;
    }

    private GitCommitData readCommit(Path repoPath, String sha, int maxDiffCharacters) {
        String metadata = runGit(repoPath, "show", "-s",
                "--format=%H" + FIELD_SEPARATOR + "%h" + FIELD_SEPARATOR + "%an" + FIELD_SEPARATOR + "%aI"
                        + FIELD_SEPARATOR + "%s" + FIELD_SEPARATOR + "%b",
                sha);
        String[] parts = metadata.split(FIELD_SEPARATOR, 6);
        if (parts.length < 6) {
            throw new LLMException("Unable to parse git metadata for commit " + sha, 500, "git_metadata_error");
        }

        String diff = runGit(repoPath, "show", "--stat", "--patch", "--unified=1", "--no-color", EMPTY_GIT_FORMAT, sha);
        String trimmedDiff = trimToLength(diff, maxDiffCharacters);

        return GitCommitData.builder()
                .sha(parts[0].trim())
                .shortSha(parts[1].trim())
                .author(parts[2].trim())
                .committedAt(parts[3].trim())
                .subject(parts[4].trim())
                .body(parts[5].trim())
                .diff(trimmedDiff)
                .build();
    }

    private List<String> readCommitShas(Path repoPath, String range, int maxCommits) {
        String output = runGit(repoPath, "rev-list", "--reverse", "--max-count=" + maxCommits, range);
        return Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private Path validateRepoPath(String repoPathValue) {
        if (repoPathValue == null || repoPathValue.isBlank()) {
            throw new LLMException("repoPath is required", 400, "validation_error");
        }
        Path repoPath = Path.of(repoPathValue).toAbsolutePath().normalize();
        if (!isUnderAllowedRoot(repoPath)) {
            throw new LLMException("repoPath is outside the configured allowed repository roots", 400, "invalid_repo_path");
        }
        if (!Files.isDirectory(repoPath)) {
            throw new LLMException("repoPath does not exist or is not a directory", 400, "invalid_repo_path");
        }
        String topLevel = readOptional(repoPath, "rev-parse", "--show-toplevel");
        if (topLevel == null || topLevel.isBlank()) {
            throw new LLMException("repoPath is not a git repository", 400, "invalid_git_repo");
        }
        return repoPath;
    }

    private boolean isUnderAllowedRoot(Path repoPath) {
        List<String> allowedRoots = releaseNotesProperties.getAllowedRepoRoots();
        if (allowedRoots == null || allowedRoots.isEmpty()) {
            return false;
        }
        for (String allowedRoot : allowedRoots) {
            if (allowedRoot == null || allowedRoot.isBlank()) {
                continue;
            }
            Path normalizedRoot = Path.of(allowedRoot).toAbsolutePath().normalize();
            if (repoPath.startsWith(normalizedRoot)) {
                return true;
            }
        }
        return false;
    }

    private int normalizeMaxCommits(Integer maxCommits) {
        if (maxCommits == null) {
            return 20;
        }
        if (maxCommits < 1 || maxCommits > 100) {
            throw new LLMException("maxCommits must be between 1 and 100", 400, "validation_error");
        }
        return maxCommits;
    }

    private int normalizeMaxDiffCharacters(Integer maxDiffCharacters) {
        if (maxDiffCharacters == null) {
            return 6000;
        }
        if (maxDiffCharacters < 500 || maxDiffCharacters > 20000) {
            throw new LLMException("maxDiffCharacters must be between 500 and 20000", 400, "validation_error");
        }
        return maxDiffCharacters;
    }

    private boolean refExists(Path repoPath, String ref) {
        return readOptional(repoPath, "rev-parse", "--verify", ref) != null;
    }

    private void verifyRefExists(Path repoPath, String ref, String label) {
        if (!refExists(repoPath, ref)) {
            throw new LLMException(label + " '" + ref + "' was not found in the git repository", 400, "git_ref_not_found");
        }
    }

    private String trimToLength(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n\n[diff truncated]";
    }

    private String readOptional(Path repoPath, String... args) {
        try {
            return runGitCommand(repoPath, args, false);
        } catch (LLMException ex) {
            return null;
        }
    }

    private String runGit(Path repoPath, String... args) {
        return runGitCommand(repoPath, args, true);
    }

    private String runGitCommand(Path repoPath, String[] args, boolean failOnError) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repoPath.toString());
        command.add("--no-pager");
        command.addAll(Arrays.asList(args));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = readOutput(process);
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new LLMException("Timed out while running git command", 500, "git_timeout");
            }

            if (process.exitValue() != 0) {
                if (!failOnError) {
                    return null;
                }
                log.warn("Git command failed: {}", output);
                throw new LLMException("Unable to read git repository data", 400, "git_command_failed");
            }
            return output;
        } catch (IOException e) {
            throw new LLMException("Failed to execute git command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("Git command was interrupted", 500, "git_interrupted");
        }
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString().trim();
    }
}
