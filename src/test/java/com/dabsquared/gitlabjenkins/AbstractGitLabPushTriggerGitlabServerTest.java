package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.handlers.PushRequestHandler;
import com.dabsquared.gitlabjenkins.models.request.GitLabPushRequest;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples;
import hudson.model.Job;
import hudson.plugins.git.RevisionParameterAction;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractGitLabPushTriggerGitlabServerTest {

	protected GitLabPushTrigger pushTrigger;
	protected GitLabPushRequestSamples gitLabPushRequestSamples;
	protected Job<?, ?> job = null;

	@Test
	public void createRevisionParameterAction_pushBrandNewMasterBranchRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.pushBrandNewMasterBranchRequest();

		// when
		RevisionParameterAction revisionParameterAction = PushRequestHandler.createPushRequestRevisionParameter(job,
				pushRequest);

		// then
		assertThat(revisionParameterAction, is(notNullValue()));
		assertThat(revisionParameterAction.commit, is(pushRequest.getAfter()));
	}

	@Test
	public void createRevisionParameterAction_mergeRequestMergePushRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.mergePushRequest();

		// when
		RevisionParameterAction revisionParameterAction = PushRequestHandler.createPushRequestRevisionParameter(job,
				pushRequest);

		// then
		assertThat(revisionParameterAction, is(notNullValue()));
		assertThat(revisionParameterAction.commit, is(pushRequest.getAfter()));
	}

	@Test
	public void createRevisionParameterAction_pushCommitRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.pushCommitRequest();

		// when
		RevisionParameterAction revisionParameterAction = PushRequestHandler.createPushRequestRevisionParameter(job,
				pushRequest);

		// then
		assertThat(revisionParameterAction, is(notNullValue()));
		assertThat(revisionParameterAction.commit, is(pushRequest.getAfter()));
	}

	@Test
	public void createRevisionParameterAction_pushNewBranchRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.pushNewBranchRequest();

		// when
		RevisionParameterAction revisionParameterAction = PushRequestHandler.createPushRequestRevisionParameter(job,
				pushRequest);

		// then
		assertThat(revisionParameterAction, is(notNullValue()));
		assertThat(revisionParameterAction.commit, is(pushRequest.getAfter()));
	}

	@Test
	public void createRevisionParameterAction_pushNewTagRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.pushNewTagRequest();

		// when
		RevisionParameterAction revisionParameterAction = PushRequestHandler.createPushRequestRevisionParameter(job,
				pushRequest);

		// then
		assertThat(revisionParameterAction, is(notNullValue()));
		assertThat(revisionParameterAction.commit, is(pushRequest.getAfter()));
	}

	@Test
	public void doNotCreateRevisionParameterAction_deleteBranchRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.deleteBranchRequest();

		// when
		RevisionParameterAction revisionParameterAction = PushRequestHandler.createPushRequestRevisionParameter(job,
				pushRequest);

		// then
		assertThat(revisionParameterAction, is(nullValue()));
	}

	protected GitLabPushTrigger setUpWithPushTrigger() {
		boolean triggerOnPush = true;
		boolean triggerOnMergeRequest = true;
		String triggerOpenMergeRequestOnPush = "both";
		boolean ciSkip = false;
		boolean setBuildDescription = true;
		boolean addNoteOnMergeRequest = true;
		boolean addCiMessage = true;
		boolean addVoteOnMergeRequest = true;
		boolean acceptMergeRequestOnSuccess = false;
		boolean allowAllBranches = true;
		String includeBranchesSpec = null;
		String excludeBranchesSpec = null;
		String buildAbortedMsg = null;
		String buildSuccessMsg = null;
		String buildFailureMsg = null;
		String buildUnstableMsg = null;
		String mergeRequestAcceptMsg = null;
		boolean customMessages = false;

		return new GitLabPushTrigger(triggerOnPush, triggerOnMergeRequest,
				triggerOpenMergeRequestOnPush, ciSkip, setBuildDescription, addNoteOnMergeRequest, addCiMessage,
				addVoteOnMergeRequest, acceptMergeRequestOnSuccess, allowAllBranches, includeBranchesSpec,
				excludeBranchesSpec, buildAbortedMsg, buildSuccessMsg, buildFailureMsg, mergeRequestAcceptMsg, buildUnstableMsg, customMessages);
	}

}
