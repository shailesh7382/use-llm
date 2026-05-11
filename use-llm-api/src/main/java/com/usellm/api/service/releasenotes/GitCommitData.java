package com.usellm.api.service.releasenotes;

public class GitCommitData {

    private String sha;
    private String shortSha;
    private String author;
    private String committedAt;
    private String subject;
    private String body;
    private String diff;

    public GitCommitData() {
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String sha;
        private String shortSha;
        private String author;
        private String committedAt;
        private String subject;
        private String body;
        private String diff;

        private Builder() {
        }

        public Builder sha(String sha) { this.sha = sha; return this; }
        public Builder shortSha(String shortSha) { this.shortSha = shortSha; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder committedAt(String committedAt) { this.committedAt = committedAt; return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder diff(String diff) { this.diff = diff; return this; }

        public GitCommitData build() {
            GitCommitData data = new GitCommitData();
            data.sha = sha;
            data.shortSha = shortSha;
            data.author = author;
            data.committedAt = committedAt;
            data.subject = subject;
            data.body = body;
            data.diff = diff;
            return data;
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
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getDiff() { return diff; }
    public void setDiff(String diff) { this.diff = diff; }
}
