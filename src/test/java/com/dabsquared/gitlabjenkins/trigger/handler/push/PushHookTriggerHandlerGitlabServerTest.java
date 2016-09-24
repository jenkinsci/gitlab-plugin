package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_10_5_489b413;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_5_1_36679b5;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_8_1_2_8c8af7b;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.eclipse.jgit.transport.RemoteConfig;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class PushHookTriggerHandlerGitlabServerTest {

    @DataPoints
    public static GitLabPushRequestSamples[] samples = {
            new GitLabPushRequestSamples_7_5_1_36679b5(),
            new GitLabPushRequestSamples_7_10_5_489b413(),
            new GitLabPushRequestSamples_8_1_2_8c8af7b()
    };

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Theory
    public void createRevisionParameterAction_pushBrandNewMasterBranchRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushBrandNewMasterBranchRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_mergeRequestMergePushRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.mergePushRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushCommitRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushNewBranchRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewBranchRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushNewTagRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewTagRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void doNotCreateRevisionParameterAction_deleteBranchRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.deleteBranchRequest();

        exception.expect(NoRevisionToBuildException.class);
        new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);
    }

    @Theory
    public void createRevisionParameterAction_pushCommitRequestWithGitScm(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        GitSCM gitSCM = new GitSCM("git@test.tld:test.git");
        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, gitSCM);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getRef().replaceFirst("^refs/heads", "remotes/origin")));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }
}
