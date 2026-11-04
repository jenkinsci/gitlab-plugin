package com.dabsquared.gitlabjenkins.trigger.handler.push;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OpenMergeRequestPushHookTriggerHandlerTest {

    @Test
    void triggeredByUser_isAuthorOfFirstCommit() {
        PushHook hook = pushHook()
                .withUserName("Alice")
                .withCommits(Arrays.asList(
                        commit().withAuthor(user().withName("Bob").build()).build(),
                        commit().withAuthor(user().withName("Carol").build()).build()))
                .build();

        assertThat(OpenMergeRequestPushHookTriggerHandler.retrieveTriggeredByUser(hook), is("Bob"));
    }

    @Test
    void emptyCommits_fallsBackToHookUserName() {
        PushHook hook = pushHook()
                .withUserName("Alice")
                .withUserUsername("alice")
                .withCommits(Collections.emptyList())
                .build();

        assertThat(OpenMergeRequestPushHookTriggerHandler.retrieveTriggeredByUser(hook), is("Alice"));
    }

    @Test
    void missingCommits_fallsBackToHookUserUsername() {
        PushHook hook = pushHook().withUserUsername("alice").build();

        assertThat(OpenMergeRequestPushHookTriggerHandler.retrieveTriggeredByUser(hook), is("alice"));
    }

    @Test
    void noUserInformation_keepsTheTriggeringUserNonNull() {
        PushHook hook = pushHook().withCommits(Collections.emptyList()).build();

        assertThat(OpenMergeRequestPushHookTriggerHandler.retrieveTriggeredByUser(hook), is(""));
    }

    @Test
    void causeDataBuildsEvenWithoutAnyUserInformation() {
        // CauseData rejects a null triggering user, so the fallback has to survive the real builder.
        PushHook hook = pushHook().withCommits(Collections.emptyList()).build();

        CauseData cause = causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(1)
                .withTargetProjectId(2)
                .withBranch("main")
                .withSourceBranch("feature")
                .withUserName("")
                .withSourceRepoHomepage("https://gitlab.org/test")
                .withSourceRepoName("test")
                .withSourceNamespace("test-namespace")
                .withSourceRepoUrl("git@gitlab.org:test.git")
                .withSourceRepoSshUrl("git@gitlab.org:test.git")
                .withSourceRepoHttpUrl("https://gitlab.org/test.git")
                .withMergeCommitSha("02b9daa1d4f7")
                .withMergeRequestTitle("Test")
                .withMergeRequestDescription("Test description")
                .withMergeRequestId(1)
                .withMergeRequestIid(1)
                .withMergeRequestTargetProjectId(2)
                .withTargetBranch("main")
                .withTargetRepoName("test")
                .withTargetNamespace("test-namespace")
                .withTargetRepoSshUrl("git@gitlab.org:test.git")
                .withTargetRepoHttpUrl("https://gitlab.org/test.git")
                .withTriggeredByUser(OpenMergeRequestPushHookTriggerHandler.retrieveTriggeredByUser(hook))
                .withLastCommit("02b9daa1d4f7")
                .withTargetProjectUrl("https://gitlab.org/test")
                .build();

        assertThat(cause.getTriggeredByUser(), is(""));
    }
}
