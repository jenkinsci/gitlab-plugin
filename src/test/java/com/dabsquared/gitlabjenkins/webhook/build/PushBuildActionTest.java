package com.dabsquared.gitlabjenkins.webhook.build;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import java.io.IOException;
import java.util.Collections;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.traits.IgnoreOnPushNotificationTrait;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public class PushBuildActionTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private StaplerResponse response;

    @Mock
    private GitLabPushTrigger trigger;

    @Test
    public void skip_missingRepositoryUrl() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        new PushBuildAction(testProject, getJson("PushEvent_missingRepositoryUrl.json"), null).execute(response);

        verify(trigger, never()).onPost(any(PushHook.class));
    }

    @Test
    public void build() throws IOException {
        try {
            FreeStyleProject testProject = jenkins.createFreeStyleProject();
            when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn(TriggerOpenMergeRequest.never);
            testProject.addTrigger(trigger);

            exception.expect(HttpResponses.HttpResponseException.class);
            new PushBuildAction(testProject, getJson("PushEvent.json"), null).execute(response);
        } finally {
            ArgumentCaptor<PushHook> pushHookArgumentCaptor = ArgumentCaptor.forClass(PushHook.class);
            verify(trigger).onPost(pushHookArgumentCaptor.capture());
            assertThat(pushHookArgumentCaptor.getValue().getProject(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getProject().getWebUrl(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getUserUsername(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getUserUsername(), containsString("jsmith"));
        }
    }

    @Test
    public void invalidToken() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn(TriggerOpenMergeRequest.never);
        when(trigger.getSecretToken()).thenReturn("secret");
        testProject.addTrigger(trigger);

        exception.expect(HttpResponses.HttpResponseException.class);
        new PushBuildAction(testProject, getJson("PushEvent.json"), "wrong-secret").execute(response);

        verify(trigger, never()).onPost(any(PushHook.class));
    }

    private String getJson(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name));
    }

    @Test
    public void scmSourceOnUpdateExecuted() {
        GitSCMSource source = new GitSCMSource("http://test");
        SCMSourceOwner item = mock(SCMSourceOwner.class);
        when(item.getSCMSources()).thenReturn(Collections.singletonList(source));
        Assert.assertThrows(
                HttpResponses.HttpResponseException.class,
                () -> new PushBuildAction(item, getJson("PushEvent.json"), null).execute(response));
        verify(item).onSCMSourceUpdated(isA(GitSCMSource.class));
    }

    @Test
    public void scmSourceOnUpdateNotExecuted() {
        GitSCMSource source = new GitSCMSource("http://test");
        source.getTraits().add(new IgnoreOnPushNotificationTrait());
        SCMSourceOwner item = mock(SCMSourceOwner.class);
        when(item.getSCMSources()).thenReturn(Collections.singletonList(source));
        Assert.assertThrows(
                HttpResponses.HttpResponseException.class,
                () -> new PushBuildAction(item, getJson("PushEvent.json"), null).execute(response));
        verify(item, never()).onSCMSourceUpdated(isA(GitSCMSource.class));
    }
}
