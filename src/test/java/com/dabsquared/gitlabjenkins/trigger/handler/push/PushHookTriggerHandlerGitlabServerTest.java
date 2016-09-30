package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_10_5_489b413;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_5_1_36679b5;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_8_1_2_8c8af7b;
import com.dabsquared.gitlabjenkins.trigger.WebHookRevisionParameterAction;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.jgit.transport.URIish;

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

        WebHookRevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.getSourceCommit(), is(hook.getAfter()));
     }

    @Theory
    public void createRevisionParameterAction_mergeRequestMergePushRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.mergePushRequest();

        WebHookRevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.getSourceCommit(), is(hook.getAfter()));
     }

    @Theory
    public void createRevisionParameterAction_pushCommitRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        WebHookRevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.getSourceCommit(), is(hook.getAfter()));
     }

    @Theory
    public void createRevisionParameterAction_pushNewBranchRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewBranchRequest();

        WebHookRevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.getSourceCommit(), is(hook.getAfter()));
    }

    @Theory
    public void createRevisionParameterAction_pushNewTagRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewTagRequest();

        WebHookRevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.getSourceCommit(), is(hook.getAfter()));
     }

    @Theory
    public void doNotCreateRevisionParameterAction_deleteBranchRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.deleteBranchRequest();

        exception.expect(NoRevisionToBuildException.class);
        new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);
    }

    @Theory
    public void createRevisionParameterAction_pushCommitRequestWith2Remotes(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        WebHookRevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.getSourceCommit(), is(hook.getAfter()));
        assertThat(revisionParameterAction.getRepoURI(), is(new URIish(hook.getRepository().getUrl())));
    }
}
