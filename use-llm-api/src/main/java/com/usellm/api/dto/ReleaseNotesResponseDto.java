package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseNotesResponseDto {

    private String repoPath;
    private String branch;
    private String baseRef;
    private String model;
    private Integer commitCount;
    private List<CommitAnalysisDto> commits;
    private String releaseNotes;

    public ReleaseNotesResponseDto() {
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String repoPath;
        private String branch;
        private String baseRef;
        private String model;
        private Integer commitCount;
        private List<CommitAnalysisDto> commits;
        private String releaseNotes;

        private Builder() {
        }

        public Builder repoPath(String repoPath) { this.repoPath = repoPath; return this; }
        public Builder branch(String branch) { this.branch = branch; return this; }
        public Builder baseRef(String baseRef) { this.baseRef = baseRef; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder commitCount(Integer commitCount) { this.commitCount = commitCount; return this; }
        public Builder commits(List<CommitAnalysisDto> commits) { this.commits = commits; return this; }
        public Builder releaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; return this; }

        public ReleaseNotesResponseDto build() {
            ReleaseNotesResponseDto dto = new ReleaseNotesResponseDto();
            dto.repoPath = repoPath;
            dto.branch = branch;
            dto.baseRef = baseRef;
            dto.model = model;
            dto.commitCount = commitCount;
            dto.commits = commits;
            dto.releaseNotes = releaseNotes;
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
    public Integer getCommitCount() { return commitCount; }
    public void setCommitCount(Integer commitCount) { this.commitCount = commitCount; }
    public List<CommitAnalysisDto> getCommits() { return commits; }
    public void setCommits(List<CommitAnalysisDto> commits) { this.commits = commits; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
}
