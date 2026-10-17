package com.dabsquared.gitlabjenkins.trigger.handler.push;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.PushHookBuilder.pushHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void noUserInformation_returnsNull() {
        PushHook hook = pushHook().withCommits(Collections.emptyList()).build();

        assertNull(OpenMergeRequestPushHookTriggerHandler.retrieveTriggeredByUser(hook));
    }
}
