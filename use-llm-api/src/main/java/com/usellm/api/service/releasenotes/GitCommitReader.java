package com.usellm.api.service.releasenotes;

import com.usellm.api.dto.ReleaseNotesRequestDto;

import java.util.List;

public interface GitCommitReader {

    List<GitCommitData> readCommits(ReleaseNotesRequestDto request);

    String resolveEffectiveBaseRef(ReleaseNotesRequestDto request);
}
