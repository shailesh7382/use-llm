package com.usellm.api.service.releasenotes;

import com.usellm.api.config.ReleaseNotesProperties;
import com.usellm.api.dto.ReleaseNotesRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellGitCommitReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsBranchCommitsAndDiffs() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        run(repo, "git", "init", "-b", "main");
        run(repo, "git", "config", "user.name", "Test User");
        run(repo, "git", "config", "user.email", "test@example.com");

        Files.writeString(repo.resolve("README.md"), "hello\n", StandardCharsets.UTF_8);
        run(repo, "git", "add", "README.md");
        run(repo, "git", "commit", "-m", "Initial commit");

        run(repo, "git", "checkout", "-b", "feature/release-notes");
        Files.writeString(repo.resolve("README.md"), "hello\nrelease notes\n", StandardCharsets.UTF_8);
        run(repo, "git", "add", "README.md");
        run(repo, "git", "commit", "-m", "Add release notes support", "-m", "Captures branch changes.");

        ShellGitCommitReader reader = new ShellGitCommitReader(new ReleaseNotesProperties());
        ReleaseNotesRequestDto request = ReleaseNotesRequestDto.builder()
                .repoPath(repo.toString())
                .branch("feature/release-notes")
                .baseRef("main")
                .maxCommits(10)
                .maxDiffCharacters(5000)
                .build();

        List<GitCommitData> commits = reader.readCommits(request);

        assertEquals(1, commits.size());
        assertEquals("Add release notes support", commits.get(0).getSubject());
        assertTrue(commits.get(0).getBody().contains("Captures branch changes."));
        assertTrue(commits.get(0).getDiff().contains("release notes"));
    }

    private void run(Path directory, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(directory.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + "\n" + output);
        }
    }
}
