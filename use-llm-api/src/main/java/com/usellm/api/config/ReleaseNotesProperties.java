package com.usellm.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "llm.release-notes")
public class ReleaseNotesProperties {

    private List<String> allowedRepoRoots = defaultAllowedRepoRoots();

    public List<String> getAllowedRepoRoots() {
        return allowedRepoRoots;
    }

    public void setAllowedRepoRoots(List<String> allowedRepoRoots) {
        this.allowedRepoRoots = allowedRepoRoots;
    }

    private List<String> defaultAllowedRepoRoots() {
        LinkedHashSet<String> roots = new LinkedHashSet<>();
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            Path currentDir = Path.of(userDir).toAbsolutePath().normalize();
            roots.add(currentDir.toString());
            Path parent = currentDir.getParent();
            if (parent != null) {
                roots.add(parent.toString());
            }
        }

        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir != null && !tempDir.isBlank()) {
            roots.add(Path.of(tempDir).toAbsolutePath().normalize().toString());
        }

        return new ArrayList<>(roots);
    }
}
