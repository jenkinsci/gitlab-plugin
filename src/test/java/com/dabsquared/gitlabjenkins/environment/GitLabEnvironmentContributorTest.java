package com.dabsquared.gitlabjenkins.environment;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.EnvVars;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.TextAxis;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleBuild;
import hudson.model.StreamBuildListener;
import jenkins.model.Jenkins;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Evgeni Golov
 */
public class GitLabEnvironmentContributorTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private BuildListener listener;

    @Before
    public void setup() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
    }

    @Test
    public void freeStyleProjectTest() throws IOException, InterruptedException, ExecutionException {
        FreeStyleProject p = jenkins.createFreeStyleProject();
        GitLabWebHookCause cause = new GitLabWebHookCause(generateCauseData());
        FreeStyleBuild b = p.scheduleBuild2(0, cause).get();
        EnvVars env = b.getEnvironment(listener);

        assertEnv(env);
    }

    @Test
    public void matrixProjectTest() throws IOException, InterruptedException, ExecutionException {
        EnvVars env;
        MatrixProject p = jenkins.jenkins.createProject(MatrixProject.class, "matrixbuild");
        GitLabWebHookCause cause = new GitLabWebHookCause(generateCauseData());
        // set up 2x2 matrix
        AxisList axes = new AxisList();
        axes.add(new TextAxis("db","mysql","oracle"));
        axes.add(new TextAxis("direction","north","south"));
        p.setAxes(axes);

        MatrixBuild build = p.scheduleBuild2(0, cause).get();
        List<MatrixRun> runs = build.getRuns();
        assertEquals(4,runs.size());
        for (MatrixRun run : runs) {
            env = run.getEnvironment(listener);
            assertNotNull(env.get("db"));
            assertEnv(env);
        }
    }

    private CauseData generateCauseData() {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(1)
                .withTargetProjectId(1)
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
                .withLastCommit("123")
                .withTargetProjectUrl("https://gitlab.org/test")
                .build();
    }

    private void assertEnv(EnvVars env) {
        assertEquals("1", env.get("gitlabMergeRequestId"));
        assertEquals("git@gitlab.org:test.git", env.get("gitlabSourceRepoUrl"));
        assertEquals("master", env.get("gitlabTargetBranch"));
        assertEquals("test", env.get("gitlabTargetRepoName"));
        assertEquals("feature", env.get("gitlabSourceBranch"));
        assertEquals("test", env.get("gitlabSourceRepoName"));
    }
}
