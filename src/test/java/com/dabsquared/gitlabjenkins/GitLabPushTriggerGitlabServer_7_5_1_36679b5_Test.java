package com.dabsquared.gitlabjenkins;

import org.junit.Before;

import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_5_1_36679b5;

public class GitLabPushTriggerGitlabServer_7_5_1_36679b5_Test extends AbstractGitLabPushTriggerGitlabServerTest {

	@Before
	public void setUp() {
		pushTrigger = setUpWithPushTrigger();
		gitLabPushRequestSamples = new GitLabPushRequestSamples_7_5_1_36679b5();
	}

}
