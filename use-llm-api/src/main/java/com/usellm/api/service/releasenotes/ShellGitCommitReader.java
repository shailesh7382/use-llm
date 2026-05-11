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

        log.info("Reading commits: repoPath={}, branch={}, baseRef={}, maxCommits={}, maxDiffChars={}",
                repoPath, branch, effectiveBaseRef != null ? effectiveBaseRef : "(none)", maxCommits, maxDiffCharacters);

        verifyRefExists(repoPath, branch, "branch");
        if (effectiveBaseRef != null) {
            verifyRefExists(repoPath, effectiveBaseRef, "baseRef");
        }

        String range = effectiveBaseRef != null ? effectiveBaseRef + ".." + branch : branch;
        log.info("Resolved git range: {}", range);
        List<String> shas = readCommitShas(repoPath, range, maxCommits);
        if (shas.isEmpty()) {
            log.warn("No commits found for range: {}", range);
            throw new LLMException("No commits found for the requested branch range", 400, "git_range_empty");
        }
        log.info("Found {} commit SHA(s) in range '{}'", shas.size(), range);

        List<GitCommitData> commits = new ArrayList<>();
        for (String sha : shas) {
            commits.add(readCommit(repoPath, sha, maxDiffCharacters));
        }
        log.info("Commits read successfully: repoPath={}, branch={}, count={}", repoPath, branch, commits.size());
        return commits;
    }

    @Override
    public String resolveEffectiveBaseRef(ReleaseNotesRequestDto request) {
        if (request.getBaseRef() != null && !request.getBaseRef().isBlank()) {
            log.info("Using explicit baseRef: {}", request.getBaseRef().trim());
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
                log.info("Resolved baseRef from origin/HEAD: {}", normalized);
                return normalized;
            }
        }

        if (!"main".equals(branch) && refExists(repoPath, "main")) {
            log.info("Resolved baseRef to default branch 'main'");
            return "main";
        }
        if (!"master".equals(branch) && refExists(repoPath, "master")) {
            log.info("Resolved baseRef to default branch 'master'");
            return "master";
        }
        log.info("No baseRef could be resolved; using full branch history");
        return null;
    }

    private GitCommitData readCommit(Path repoPath, String sha, int maxDiffCharacters) {
        log.info("Reading commit data: sha={}", sha);
        String metadata = runGit(repoPath, "show", "-s",
                "--format=%H" + FIELD_SEPARATOR + "%h" + FIELD_SEPARATOR + "%an" + FIELD_SEPARATOR + "%aI"
                        + FIELD_SEPARATOR + "%s" + FIELD_SEPARATOR + "%b",
                sha);
        String[] parts = metadata.split(FIELD_SEPARATOR, -1);
        if (parts.length < 5) {
            log.error("Failed to parse git metadata for commit {}: parts={}", sha, parts.length);
            throw new LLMException("Unable to parse git metadata for commit " + sha, 500, "git_metadata_error");
        }

        String diff = runGit(repoPath, "show", "--stat", "--patch", "--unified=1", "--no-color", EMPTY_GIT_FORMAT, sha);
        String trimmedDiff = trimToLength(diff, maxDiffCharacters);
        boolean diffTrimmed = diff != null && diff.length() > maxDiffCharacters;

        GitCommitData data = GitCommitData.builder()
                .sha(parts[0].trim())
                .shortSha(parts[1].trim())
                .author(parts[2].trim())
                .committedAt(parts[3].trim())
                .subject(parts[4].trim())
                .body(parts.length > 5 ? parts[5].trim() : "")
                .diff(trimmedDiff)
                .build();

        log.info("Commit read: sha={}, author='{}', subject='{}', diffChars={}, diffTrimmed={}",
                data.getShortSha(), data.getAuthor(), data.getSubject(),
                trimmedDiff != null ? trimmedDiff.length() : 0, diffTrimmed);
        return data;
    }

    private List<String> readCommitShas(Path repoPath, String range, int maxCommits) {
        log.info("Reading commit SHAs: range='{}', maxCommits={}", range, maxCommits);
        String output = runGit(repoPath, "rev-list", "--reverse", "--max-count=" + maxCommits, range);
        List<String> shas = Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        log.info("Commit SHAs retrieved: range='{}', count={}", range, shas.size());
        return shas;
    }

    private Path validateRepoPath(String repoPathValue) {
        if (repoPathValue == null || repoPathValue.isBlank()) {
            throw new LLMException("repoPath is required", 400, "validation_error");
        }
        Path repoPath = Path.of(repoPathValue).toAbsolutePath().normalize();
        log.info("Validating repoPath: {}", repoPath);
        if (!isUnderAllowedRoot(repoPath)) {
            log.warn("repoPath '{}' is outside allowed roots", repoPath);
            throw new LLMException("repoPath is outside the configured allowed repository roots", 400, "invalid_repo_path");
        }
        if (!Files.isDirectory(repoPath)) {
            log.warn("repoPath '{}' does not exist or is not a directory", repoPath);
            throw new LLMException("repoPath does not exist or is not a directory", 400, "invalid_repo_path");
        }
        String topLevel = readOptional(repoPath, "rev-parse", "--show-toplevel");
        if (topLevel == null || topLevel.isBlank()) {
            log.warn("repoPath '{}' is not a git repository", repoPath);
            throw new LLMException("repoPath is not a git repository", 400, "invalid_git_repo");
        }
        log.info("repoPath validated: {}, git top-level='{}'", repoPath, topLevel.trim());
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
        log.info("Verifying ref exists: {}='{}'", label, ref);
        if (!refExists(repoPath, ref)) {
            log.warn("Git ref not found: {}='{}'", label, ref);
            throw new LLMException(label + " '" + ref + "' was not found in the git repository", 400, "git_ref_not_found");
        }
        log.info("Git ref verified: {}='{}'", label, ref);
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

        log.info("Executing git command: {}", command);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = readOutput(process);
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("Git command timed out ({}s): {}", COMMAND_TIMEOUT.toSeconds(), command);
                throw new LLMException("Timed out while running git command", 500, "git_timeout");
            }

            int exitCode = process.exitValue();
            log.info("Git command exited: code={}, outputLength={}", exitCode, output.length());

            if (exitCode != 0) {
                if (!failOnError) {
                    return null;
                }
                log.warn("Git command failed (exit={}): output={}", exitCode, output);
                throw new LLMException("Unable to read git repository data", 400, "git_command_failed");
            }
            return output;
        } catch (IOException e) {
            log.error("IOException executing git command: {}", e.getMessage());
            throw new LLMException("Failed to execute git command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Git command interrupted: {}", command);
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
