package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.gitlab.api.model.PushHook;

import java.util.Arrays;
import java.util.Collections;

import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.builder.generated.RepositoryBuilder.repository;

public class GitLabPushRequestSamples_7_5_1_36679b5 implements GitLabPushRequestSamples {

    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";

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
                .withAfter("d91a0f248625f6dc808fb7cda75c4ee01516b609")
                // no checkout_sha and no commit on new branches
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
                .withAfter("2bf4170829aedd706d7485d40091a01637b9abf4")
                // no checkout_sha and no commit on new branches
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
                .withBefore("2bf4170829aedd706d7485d40091a01637b9abf4")
                .withAfter("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb")
                // no checkout_sha
                .withCommits(Collections.singletonList(commit().withId("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb").build()))
                .build();
    }

    public PushHook mergePushRequest() {
        return pushHook().withRef("refs/heads/master")
                .withUserId(123)
                .withUserName("admin@example")
                .withProjectId(345)
                .withRepository(repository()
                        .withName("test-repo")
                        .withUrl("git@gitlabserver.example.com:test-group/test-repo.git")
                        .withHomepage("http://gitlabserver.example.com/test-group/test-repo")
                        .build())
                .withBefore("27548e742f40971f75c715aaa7920404eeff6616")
                .withAfter("3ebb6927ad4afbe8a11830938b3584cdaf4d657b")
                // no checkout_sha
                .withCommits(Arrays.asList(
                        commit().withId("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb").build(),
                        commit().withId("be473fcc670b920cc9795581a5cd8f00fa7afddd").build(),
                        commit().withId("3ebb6927ad4afbe8a11830938b3584cdaf4d657b").build())
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
                .withBefore(ZERO_SHA).withAfter("f10d9d7b648e5a3e55fe8fe865aba5aa7404df7c")
                // no checkout_sha and no commit on new branches
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
                .withBefore("3ebb6927ad4afbe8a11830938b3584cdaf4d657b").withAfter(ZERO_SHA)
                // no checkout_sha and no commit on new branches
                .build();
    }
}
