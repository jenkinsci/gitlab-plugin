package com.dabsquared.gitlabjenkins.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.CauseDataBuilder;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.workflow.GitLabBranchBuild;
import hudson.EnvVars;
import hudson.Functions;
import hudson.Util;
import hudson.model.Cause;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.eclipse.jgit.lib.ObjectId;
import org.gitlab4j.api.CommitsApi;
import org.gitlab4j.api.Constants.CommitBuildState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.CommitStatus;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author Daumantas Stulgis
 */
public class CommitStatusUpdaterTest {

    private static final Long PROJECT_ID = 1L;
    private static final String BUILD_URL = "job/Test-Job";
    private static final String STAGE = "test";
    private static final String REVISION = "1111111";
    private static final String JENKINS_URL = "https://gitlab.org/jenkins/";

    @Mock
    Run<?, ?> build;

    @Mock
    TaskListener taskListener;

    @Mock
    GitLabConnectionConfig gitLabConnectionConfig;

    @Mock
    GitLabApi gitLabApi;

    @Mock
    CommitsApi commitsApi;

    @Mock
    GitLabWebHookCause gitlabCause;

    @Mock
    BuildData action;

    @Mock
    Revision lastBuiltRevision;

    @Mock
    Build lastBuild;

    @Mock
    Revision revision;

    @Mock
    EnvVars environment;

    @Mock
    UpstreamCause upCauseLevel1;

    @Mock
    UpstreamCause upCauseLevel2;

    @Mock
    Jenkins jenkins;

    @Mock
    GitLabConnectionProperty connection;

    private AutoCloseable closeable;
    private MockedStatic<Jenkins> mockedJenkins;
    private MockedStatic<GitLabConnectionProperty> mockedGitLabConnectionProperty;
    private MockedStatic<DisplayURLProvider> mockedDisplayURLProvider;

    CauseData causeData;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        mockedJenkins = Mockito.mockStatic(Jenkins.class);
        mockedJenkins.when(Jenkins::getInstance).thenReturn(jenkins);
        mockedJenkins.when(Jenkins::getActiveInstance).thenReturn(jenkins);
        when(jenkins.getRootUrl()).thenReturn(JENKINS_URL);
        when(jenkins.getDescriptor(GitLabConnectionConfig.class)).thenReturn(gitLabConnectionConfig);
        mockedGitLabConnectionProperty = Mockito.mockStatic(GitLabConnectionProperty.class);
        mockedGitLabConnectionProperty
                .when(() -> GitLabConnectionProperty.getClient(any(Run.class)))
                .thenReturn(gitLabApi);
        when(gitLabConnectionConfig.getClient(any(String.class), any(Item.class), any(String.class)))
                .thenReturn(gitLabApi);
        when(connection.getClient()).thenReturn(gitLabApi);
        when(build.getAction(BuildData.class)).thenReturn(action);
        when(action.getLastBuiltRevision()).thenReturn(lastBuiltRevision);
        when(action.getLastBuild(any(ObjectId.class))).thenReturn(lastBuild);
        when(lastBuild.getMarked()).thenReturn(revision);
        when(revision.getSha1String()).thenReturn(REVISION);
        when(build.getUrl()).thenReturn(BUILD_URL);
        when(build.getEnvironment(any(TaskListener.class))).thenReturn(environment);
        when(build.getCauses()).thenReturn(new ArrayList<Cause>(Collections.singletonList(upCauseLevel1)));
        when(upCauseLevel1.getUpstreamCauses())
                .thenReturn(new ArrayList<Cause>(Collections.singletonList(upCauseLevel2)));
        when(upCauseLevel2.getUpstreamCauses())
                .thenReturn(new ArrayList<Cause>(Collections.singletonList(gitlabCause)));
        if (Functions.isWindows()) {
            when(taskListener.getLogger()).thenReturn(new PrintStream("nul"));
        } else {
            when(taskListener.getLogger()).thenReturn(new PrintStream("/dev/null"));
        }
        mockedDisplayURLProvider = Mockito.mockStatic(DisplayURLProvider.class);
        DisplayURLProvider urlProvider = mock(DisplayURLProvider.class);
        mockedDisplayURLProvider.when(DisplayURLProvider::get).thenReturn(urlProvider);
        String url = JENKINS_URL + Util.encode(build.getUrl());
        when(urlProvider.getRunURL(any())).thenReturn(url);
        when(gitLabApi.getCommitsApi()).thenReturn(commitsApi);

        causeData = CauseDataBuilder.causeData()
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
                .withMergeRequestId(1L)
                .withMergeRequestIid(1L)
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
    }

    @After
    public void tearDown() throws Exception {
        mockedDisplayURLProvider.close();
        mockedGitLabConnectionProperty.close();
        mockedJenkins.close();
        closeable.close();
    }

    @Test
    public void buildStateUpdateTest() throws GitLabApiException {
        CommitStatusUpdater.updateCommitStatus(build, taskListener, CommitBuildState.SUCCESS, STAGE);

        CommitStatus status = new CommitStatus();
        status.withRef(null)
                .withName(STAGE)
                .withCoverage((Float.valueOf(null)))
                .withTargetUrl(DisplayURLProvider.get().getRunURL(build));

        verify(commitsApi).addCommitStatus(PROJECT_ID, REVISION, CommitBuildState.SUCCESS, status);
    }

    @Test
    public void buildStateUpdateTestSpecificConnection() throws GitLabApiException {
        CommitStatusUpdater.updateCommitStatus(build, taskListener, CommitBuildState.SUCCESS, STAGE, null, connection);

        CommitStatus status = new CommitStatus();
        status.withRef(null)
                .withName(STAGE)
                .withCoverage((Float.valueOf(null)))
                .withTargetUrl(DisplayURLProvider.get().getRunURL(build));

        verify(commitsApi).addCommitStatus(Long.toString(PROJECT_ID), REVISION, CommitBuildState.SUCCESS, status);
    }

    @Test
    public void buildStateUpdateTestSpecificBuild() throws GitLabApiException {
        ArrayList builds = new ArrayList();
        builds.add(new GitLabBranchBuild(Long.toString(PROJECT_ID), REVISION));
        CommitStatusUpdater.updateCommitStatus(build, taskListener, CommitBuildState.SUCCESS, STAGE, builds, null);

        CommitStatus status = new CommitStatus();
        status.withRef(null)
                .withName(STAGE)
                .withCoverage((Float.valueOf(null)))
                .withTargetUrl(DisplayURLProvider.get().getRunURL(build));

        when(gitLabApi.getCommitsApi()).thenReturn(commitsApi);
        verify(commitsApi).addCommitStatus(PROJECT_ID, REVISION, CommitBuildState.SUCCESS, status);
    }

    @Test
    public void buildStateUpdateTestSpecificConnectionSpecificBuild() throws GitLabApiException {
        ArrayList builds = new ArrayList();
        builds.add(new GitLabBranchBuild(Long.toString(PROJECT_ID), REVISION));
        CommitStatusUpdater.updateCommitStatus(
                build, taskListener, CommitBuildState.SUCCESS, STAGE, builds, connection);

        CommitStatus status = new CommitStatus();
        status.withRef(null)
                .withName(STAGE)
                .withCoverage(Float.valueOf(null))
                .withTargetUrl(DisplayURLProvider.get().getRunURL(build));

        verify(commitsApi).addCommitStatus(Long.toString(PROJECT_ID), REVISION, CommitBuildState.SUCCESS, status);
    }

    @Test
    public void testTagEvent() throws GitLabApiException {
        causeData = CauseDataBuilder.causeData()
                .withActionType(CauseData.ActionType.TAG_PUSH)
                .withSourceProjectId(PROJECT_ID)
                .withTargetProjectId(PROJECT_ID)
                .withBranch("refs/tags/3.0.0")
                .withSourceBranch("refs/tags/3.0.0")
                .withUserName("")
                .withSourceRepoHomepage("https://gitlab.org/test")
                .withSourceRepoName("test")
                .withSourceNamespace("test-namespace")
                .withSourceRepoUrl("git@gitlab.org:test.git")
                .withSourceRepoSshUrl("git@gitlab.org:test.git")
                .withSourceRepoHttpUrl("https://gitlab.org/test.git")
                .withMergeRequestTitle("Test")
                .withMergeRequestId(1L)
                .withMergeRequestIid(1L)
                .withTargetBranch("master")
                .withTargetRepoName("test")
                .withTargetNamespace("test-namespace")
                .withTargetRepoSshUrl("git@gitlab.org:test.git")
                .withTargetRepoHttpUrl("https://gitlab.org/test.git")
                .withTriggeredByUser("test")
                .withLastCommit(REVISION)
                .withTargetProjectUrl("https://gitlab.org/test")
                .build();

        when(build.getCause(GitLabWebHookCause.class)).thenReturn(gitlabCause);
        when(gitlabCause.getData()).thenReturn(causeData);

        CommitStatusUpdater.updateCommitStatus(build, taskListener, CommitBuildState.SUCCESS, STAGE);

        CommitStatus status = new CommitStatus();
        status.withRef("3.0.0")
                .withName(STAGE)
                .withCoverage((Float.valueOf(null)))
                .withTargetUrl(DisplayURLProvider.get().getRunURL(build));

        verify(commitsApi).addCommitStatus(Long.toString(PROJECT_ID), REVISION, CommitBuildState.SUCCESS, status);
    }
}
