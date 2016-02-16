package com.dabsquared.gitlabjenkins;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples;

import hudson.model.Job;
import hudson.plugins.git.RevisionParameterAction;

public abstract class AbstractGitLabPushTriggerGitlabServerTest {

	protected GitLabPushTrigger pushTrigger;
	protected GitLabPushRequestSamples gitLabPushRequestSamples;
	protected Job<?, ?> job = null;

	@Test
	public void createRevisionParameterAction_pushBrandNewMasterBranchRequest() throws Exception {
		// given
		GitLabPushRequest pushRequest = gitLabPushRequestSamples.pushBrandNewMasterBranchRequest();

		// when
		RevisionParameterAction revisionParameterAction = pushTrigger.createPushRequestRevisionParameter(job,
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
		RevisionParameterAction revisionParameterAction = pushTrigger.createPushRequestRevisionParameter(job,
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
		RevisionParameterAction revisionParameterAction = pushTrigger.createPushRequestRevisionParameter(job,
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
		RevisionParameterAction revisionParameterAction = pushTrigger.createPushRequestRevisionParameter(job,
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
		RevisionParameterAction revisionParameterAction = pushTrigger.createPushRequestRevisionParameter(job,
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
		RevisionParameterAction revisionParameterAction = pushTrigger.createPushRequestRevisionParameter(job,
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
		String branchFilter = null;
		String includeBranchesSpec = null;
		String excludeBranchesSpec = null;
		String targetBranchRegex = null;
		GitLabPushTrigger gitLabPushTrigger = new GitLabPushTrigger(triggerOnPush, triggerOnMergeRequest,
				triggerOpenMergeRequestOnPush, ciSkip, setBuildDescription, addNoteOnMergeRequest, addCiMessage,
				addVoteOnMergeRequest, acceptMergeRequestOnSuccess, branchFilter, includeBranchesSpec,
				excludeBranchesSpec, targetBranchRegex);

		return gitLabPushTrigger;
	}

}
