package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;

import java.util.Arrays;
import java.util.Collections;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.RepositoryBuilder.repository;

public class GitLabPushRequestSamples_7_10_5_489b413 implements GitLabPushRequestSamples {

    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";
    private static final String COMMIT_7A = "7a5db3baf5d42b4218a2a501c88f745559b1d24c";
    private static final String COMMIT_21 = "21d67fe28097b49a1a6fb5c82cbfe03d389e8685";
    private static final String COMMIT_9d = "9dbdd7a128a2789d0f436222ce116f1d8229e083";

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
                .withAfter(COMMIT_7A)
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
                .withBefore(ZERO_SHA).withAfter(COMMIT_7A)
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
                .withBefore(COMMIT_7A)
                .withAfter(COMMIT_21)
                .withCommits(Collections.singletonList(commit().withId(COMMIT_21).build()))
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
                .withBefore("ca84f96a846b0e241808ea7b75dfa3bf4cd3b98b")
                .withAfter(COMMIT_9d)
                .withCommits(Arrays.asList(
                        commit().withId(COMMIT_21).build(),
                        commit().withId("c04c8822d1df397fb7e6dd3dd133018a0af567a8").build(),
                        commit().withId(COMMIT_9d).build())
                )
                .build();
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
                .withRef("refs/tags/test-tag-1")
                .withBefore(ZERO_SHA)
                .withAfter(COMMIT_21)
                .withCommits(Collections.singletonList(commit().withId(COMMIT_21).build()))
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
                .withRef("refs/heads/test-branch-3-delete")
                .withBefore("c34984ff6ed9935b3d843237947adbaaa85fc5f9").withAfter(ZERO_SHA)
                .build();
    }

}
