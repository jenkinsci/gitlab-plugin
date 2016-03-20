package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;

import java.util.Arrays;
import java.util.Collections;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;

public class GitLabPushRequestSamples_8_1_2_8c8af7b implements GitLabPushRequestSamples {

    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";
    private static final String COMMIT_25 = "258d6f6e21e6dda343f6e9f8e78c38f12bb81c87";
    private static final String COMMIT_63 = "63b30060be89f0338123f2d8975588e7d40a1874";
    private static final String COMMIT_64 = "64ed77c360ee7ac900c68292775bee2184c1e593";
    private static final String COMMIT_74 = "742d8d0b4b16792c38c6798b28ba1fa754da165e";
    private static final String COMMIT_E5 = "e5a46665b80965724b45fe921788105258b3ec5c";

    public PushHook pushBrandNewMasterBranchRequest() {
        return pushHook()
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withRef("refs/heads/master")
                .withBefore(ZERO_SHA)
                .withAfter(COMMIT_63)
                // no commit on new branches
                .build();
    }

    public PushHook pushNewBranchRequest() {
        return pushHook()
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withRef("refs/heads/test-new-branch1")
                .withBefore(ZERO_SHA)
                .withAfter(COMMIT_25)
                // no commit on new branches
                .build();
    }

    public PushHook pushCommitRequest() {

        return pushHook()
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withRef("refs/heads/test-new-branch1")
                .withBefore(COMMIT_25)
                .withAfter(COMMIT_74)
                .withCommits(Collections.singletonList(commit().withId(COMMIT_74).build()))
                .build();
    }

    public PushHook mergePushRequest() {
        return pushHook()
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withRef("refs/heads/master")
                .withBefore("e8b9327c9704e308949f9d31dd0fae6abfac3798")
                .withAfter(COMMIT_E5)
                .withCommits(Arrays.asList(
                        commit().withId(COMMIT_74).build(),
                        commit().withId("ab569fa9c51fa80d6509b277a6b587faf8e7cb72").build(),
                        commit().withId(COMMIT_E5).build())
                )
                .build();

        // and afterwards the "delete branch" request comes in
    }

    public PushHook pushNewTagRequest() {
        return pushHook()
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withRef("refs/tags/test-tag-2")
                .withBefore(ZERO_SHA)
                .withAfter(COMMIT_64)
                .withCommits(Collections.singletonList(commit().withId(COMMIT_64).build()))
                .build();
    }

    public PushHook deleteBranchRequest() {
        return pushHook()
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withRef("refs/heads/test-branch-delete-1")
                .withBefore("784c5ca7814aa7ea1913ae8e64187c31322946f0")
                .withAfter(ZERO_SHA)
                .build();
    }

}
