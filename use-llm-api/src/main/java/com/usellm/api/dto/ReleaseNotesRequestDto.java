package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseNotesRequestDto {

    @NotBlank(message = "repoPath is required")
    private String repoPath;

    @NotBlank(message = "branch is required")
    private String branch;

    private String baseRef;
    private String model;
    private Integer maxCommits = 20;
    private Integer maxDiffCharacters = 6000;

    public ReleaseNotesRequestDto() {
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String repoPath;
        private String branch;
        private String baseRef;
        private String model;
        private Integer maxCommits = 20;
        private Integer maxDiffCharacters = 6000;

        private Builder() {
        }

        public Builder repoPath(String repoPath) { this.repoPath = repoPath; return this; }
        public Builder branch(String branch) { this.branch = branch; return this; }
        public Builder baseRef(String baseRef) { this.baseRef = baseRef; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder maxCommits(Integer maxCommits) { this.maxCommits = maxCommits; return this; }
        public Builder maxDiffCharacters(Integer maxDiffCharacters) { this.maxDiffCharacters = maxDiffCharacters; return this; }

        public ReleaseNotesRequestDto build() {
            ReleaseNotesRequestDto dto = new ReleaseNotesRequestDto();
            dto.repoPath = repoPath;
            dto.branch = branch;
            dto.baseRef = baseRef;
            dto.model = model;
            dto.maxCommits = maxCommits;
            dto.maxDiffCharacters = maxDiffCharacters;
            return dto;
        }
    }

    public String getRepoPath() { return repoPath; }
    public void setRepoPath(String repoPath) { this.repoPath = repoPath; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getBaseRef() { return baseRef; }
    public void setBaseRef(String baseRef) { this.baseRef = baseRef; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getMaxCommits() { return maxCommits; }
    public void setMaxCommits(Integer maxCommits) { this.maxCommits = maxCommits; }
    public Integer getMaxDiffCharacters() { return maxDiffCharacters; }
    public void setMaxDiffCharacters(Integer maxDiffCharacters) { this.maxDiffCharacters = maxDiffCharacters; }
}
