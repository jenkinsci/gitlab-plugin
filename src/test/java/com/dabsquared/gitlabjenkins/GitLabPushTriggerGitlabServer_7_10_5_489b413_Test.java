package com.dabsquared.gitlabjenkins;

import org.junit.Before;

import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_10_5_489b413;

public class GitLabPushTriggerGitlabServer_7_10_5_489b413_Test extends AbstractGitLabPushTriggerGitlabServerTest {

	@Before
	public void setUp() {
		pushTrigger = setUpWithPushTrigger();
		gitLabPushRequestSamples = new GitLabPushRequestSamples_7_10_5_489b413();
	}

}
