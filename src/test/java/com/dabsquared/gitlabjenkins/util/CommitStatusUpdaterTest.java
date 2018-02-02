package com.dabsquared.gitlabjenkins.util;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;

import hudson.EnvVars;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import jenkins.model.Jenkins;

/**
 * @author Daumantas Stulgis
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GitLabConnectionProperty.class, Jenkins.class})
public class CommitStatusUpdaterTest {
	
	private static final int PROJECT_ID = 1;
    private static final String BUILD_URL = "job/Test-Job";
    private static final String STAGE = "test";
    private static final String REVISION = "1111111";
    private static final String JENKINS_URL = "https://gitlab.org/jenkins/";
	
    @Mock Run<?, ?> build;
	@Mock TaskListener taskListener;
	@Mock GitLabClient client;
	@Mock GitLabWebHookCause gitlabCause;
	@Mock BuildData action;
	@Mock Revision lastBuiltRevision;
	@Mock Build lastBuild;
	@Mock Revision revision;
	@Mock EnvVars environment;
	@Mock UpstreamCause upCauseLevel1;
	@Mock UpstreamCause upCauseLevel2;
	@Mock Jenkins jenkins;
	
	CauseData causeData;

	@Before
	public void setUp() throws Exception {
	    MockitoAnnotations.initMocks(this);
	    PowerMockito.mockStatic(GitLabConnectionProperty.class);
	    PowerMockito.mockStatic(Jenkins.class);
	    when(Jenkins.getInstance()).thenReturn(jenkins);
	    when(jenkins.getRootUrl()).thenReturn(JENKINS_URL);
	    when(GitLabConnectionProperty.getClient(any(Run.class))).thenReturn(client);
	    when(build.getAction(BuildData.class)).thenReturn(action);
	    when(action.getLastBuiltRevision()).thenReturn(lastBuiltRevision);
	    when(action.getLastBuild(any(ObjectId.class))).thenReturn(lastBuild);
	    when(lastBuild.getMarked()).thenReturn(revision);
	    when(revision.getSha1String()).thenReturn(REVISION);
	    when(build.getUrl()).thenReturn(BUILD_URL);
	    when(build.getEnvironment(any(TaskListener.class))).thenReturn(environment);
	    when(build.getCauses()).thenReturn(new ArrayList<Cause>(Collections.singletonList(upCauseLevel1)));
	    when(upCauseLevel1.getUpstreamCauses()).thenReturn(new ArrayList<Cause>(Collections.singletonList(upCauseLevel2)));
	    when(upCauseLevel2.getUpstreamCauses()).thenReturn(new ArrayList<Cause>(Collections.singletonList(gitlabCause)));
	    
	    causeData = causeData()
                .withActionType(CauseData.ActionType.NOTE)
                .withSourceProjectId(PROJECT_ID)
                .withTargetProjectId(PROJECT_ID)
                .withBranch("feature")
                .withSourceBranch("feature")
                .withUserName("")
                .withSourceRepoHomepage("https://gitlab.org/test")
                .withSourceRepoName("test")
                .withSourceNamespace("test-namespace")
                .withSourceRepoUrl("git@gitlab.org:test.git")
                .withSourceRepoSshUrl("git@gitlab.org:test.git")
                .withSourceRepoHttpUrl("https://gitlab.org/test.git")
                .withMergeRequestTitle("Test")
                .withMergeRequestId(1)
                .withMergeRequestIid(1)
                .withTargetBranch("master")
                .withTargetRepoName("test")
                .withTargetNamespace("test-namespace")
                .withTargetRepoSshUrl("git@gitlab.org:test.git")
                .withTargetRepoHttpUrl("https://gitlab.org/test.git")
                .withTriggeredByUser("test")
                .withLastCommit(REVISION)
                .withTargetProjectUrl("https://gitlab.org/test")
                .build();
	    
	    when(gitlabCause.getData()).thenReturn(causeData);
	    PowerMockito.spy(client);
	}

	@Test
	public void test() {
		CommitStatusUpdater.updateCommitStatus(build, taskListener, BuildState.success, STAGE);
		
		verify(client).changeBuildStatus(Integer.toString(PROJECT_ID), REVISION, BuildState.success, null, STAGE, DisplayURLProvider.get().getRunURL(build), BuildState.success.name());
	}

}
