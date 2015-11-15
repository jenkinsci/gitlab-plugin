package com.dabsquared.gitlabjenkins;

import org.junit.Before;

import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_8_1_2_8c8af7b;

public class GitLabPushTriggerGitlabServer_8_1_2_8c8af7b_Test extends AbstractGitLabPushTriggerGitlabServerTest {

	@Before
	public void setUp() {
		pushTrigger = setUpWithPushTrigger();
		gitLabPushRequestSamples = new GitLabPushRequestSamples_8_1_2_8c8af7b();
	}

}
