package com.dabsquared.gitlabjenkins.webhook.build;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import hudson.model.FreeStyleProject;
import hudson.security.ACL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.traits.IgnoreOnPushNotificationTrait;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PushBuildActionTest {

    private static JenkinsRule jenkins;

    @Mock
    private StaplerResponse2 response;

    @Mock
    private GitLabPushTrigger trigger;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void skip_missingRepositoryUrl() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        new PushBuildAction(testProject, getJson("PushEvent_missingRepositoryUrl.json"), null).execute(response);

        verify(trigger, never()).onPost(any(PushHook.class));
    }

    @Test
    void build() {
        assertThrows(HttpResponses.HttpResponseException.class, () -> {
            try {
                FreeStyleProject testProject = jenkins.createFreeStyleProject();
                when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn(TriggerOpenMergeRequest.never);
                testProject.addTrigger(trigger);

                // exception.expect(HttpResponses.HttpResponseException.class);
                new PushBuildAction(testProject, getJson("PushEvent.json"), null).execute(response);
            } finally {
                ArgumentCaptor<PushHook> pushHookArgumentCaptor = ArgumentCaptor.forClass(PushHook.class);
                verify(trigger).onPost(pushHookArgumentCaptor.capture());
                assertThat(pushHookArgumentCaptor.getValue().getProject(), is(notNullValue()));
                assertThat(pushHookArgumentCaptor.getValue().getProject().getWebUrl(), is(notNullValue()));
                assertThat(pushHookArgumentCaptor.getValue().getUserUsername(), is(notNullValue()));
                assertThat(pushHookArgumentCaptor.getValue().getUserUsername(), containsString("jsmith"));
            }
        });
    }

    @Test
    void invalidToken() {
        assertThrows(HttpResponses.HttpResponseException.class, () -> {
            FreeStyleProject testProject = jenkins.createFreeStyleProject();
            when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn(TriggerOpenMergeRequest.never);
            when(trigger.getSecretToken()).thenReturn("secret");
            testProject.addTrigger(trigger);
            new PushBuildAction(testProject, getJson("PushEvent.json"), "wrong-secret").execute(response);

            verify(trigger, never()).onPost(any(PushHook.class));
        });
    }

    private String getJson(String name) throws Exception {
        return IOUtils.toString(getClass().getResourceAsStream(name), StandardCharsets.UTF_8);
    }

    @Test
    void scmSourceOnUpdateExecuted() {
        GitSCMSource source = new GitSCMSource("http://test");
        SCMSourceOwner item = mock(SCMSourceOwner.class);
        ACL acl = mock(ACL.class);
        when(item.getSCMSources()).thenReturn(Collections.singletonList(source));
        when(item.getACL()).thenReturn(acl);
        assertThrows(
                HttpResponses.HttpResponseException.class,
                () -> new PushBuildAction(item, getJson("PushEvent.json"), null).execute(response));
        item.onSCMSourceUpdated(source);
        verify(item).onSCMSourceUpdated(isA(GitSCMSource.class));
    }

    @Test
    void scmSourceOnUpdateNotExecuted() {
        GitSCMSource source = new GitSCMSource("http://test");
        source.getTraits().add(new IgnoreOnPushNotificationTrait());
        SCMSourceOwner item = mock(SCMSourceOwner.class);
        when(item.getSCMSources()).thenReturn(Collections.singletonList(source));
        assertThrows(
                HttpResponses.HttpResponseException.class,
                () -> new PushBuildAction(item, getJson("PushEvent.json"), null).execute(response));
        verify(item, never()).onSCMSourceUpdated(isA(GitSCMSource.class));
    }
}
