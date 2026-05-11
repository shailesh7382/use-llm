package com.usellm.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommitAnalysisDto {

    private String sha;
    private String shortSha;
    private String author;
    private String committedAt;
    private String subject;
    private String summary;

    public CommitAnalysisDto() {
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String sha;
        private String shortSha;
        private String author;
        private String committedAt;
        private String subject;
        private String summary;

        private Builder() {
        }

        public Builder sha(String sha) { this.sha = sha; return this; }
        public Builder shortSha(String shortSha) { this.shortSha = shortSha; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder committedAt(String committedAt) { this.committedAt = committedAt; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }

        public CommitAnalysisDto build() {
            CommitAnalysisDto dto = new CommitAnalysisDto();
            dto.sha = sha;
            dto.shortSha = shortSha;
            dto.author = author;
            dto.committedAt = committedAt;
            dto.subject = subject;
            dto.summary = summary;
            return dto;
        }
    }

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public String getShortSha() { return shortSha; }
    public void setShortSha(String shortSha) { this.shortSha = shortSha; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCommittedAt() { return committedAt; }
    public void setCommittedAt(String committedAt) { this.committedAt = committedAt; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
