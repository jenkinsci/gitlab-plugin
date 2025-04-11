package com.dabsquared.gitlabjenkins.environment;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.CauseDataBuilder;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.EnvVars;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.TextAxis;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.StreamBuildListener;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Evgeni Golov
 */
@WithJenkins
class GitLabEnvironmentContributorTest {

    private static JenkinsRule jenkins;

    private BuildListener listener;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
    }

    @Test
    void freeStyleProjectTest() throws Exception {
        FreeStyleProject p = jenkins.createFreeStyleProject();
        GitLabWebHookCause cause = new GitLabWebHookCause(generateCauseData());
        FreeStyleBuild b = p.scheduleBuild2(0, cause).get();
        EnvVars env = b.getEnvironment(listener);

        assertEnv(env);
    }

    @Test
    void matrixProjectTest() throws Exception {
        EnvVars env;
        MatrixProject p = jenkins.jenkins.createProject(MatrixProject.class, "matrixbuild");
        GitLabWebHookCause cause = new GitLabWebHookCause(generateCauseData());
        // set up 2x2 matrix
        AxisList axes = new AxisList();
        axes.add(new TextAxis("db", "mysql", "oracle"));
        axes.add(new TextAxis("direction", "north", "south"));
        p.setAxes(axes);

        MatrixBuild build = p.scheduleBuild2(0, cause).get();
        List<MatrixRun> runs = build.getRuns();
        assertEquals(4, runs.size());
        for (MatrixRun run : runs) {
            env = run.getEnvironment(listener);
            assertNotNull(env.get("db"));
            assertEnv(env);
        }
    }

    private void testFreeStyleProjectLabels(CauseData causeData, String expected) throws Exception {
        FreeStyleProject p = jenkins.createFreeStyleProject();
        GitLabWebHookCause cause = new GitLabWebHookCause(causeData);
        FreeStyleBuild b = p.scheduleBuild2(0, cause).get();
        EnvVars env = b.getEnvironment(listener);
        assertEquals(expected, env.get("gitlabMergeRequestLabels"));
    }

    @Test
    void freeStyleProjectTestNoLabels() throws Exception {
        // withMergeRequestLabels() not called on CauseDataBuilder
        testFreeStyleProjectLabels(generateCauseData(), null);
    }

    @Test
    void freeStyleProjectTestNullLabels() throws Exception {
        // null passed as labels
        testFreeStyleProjectLabels(generateCauseDataWithLabels(null), null);
    }

    @Test
    void freeStyleProjectTestEmptyLabels() throws Exception {
        // empty list passed as labels
        testFreeStyleProjectLabels(generateCauseDataWithLabels(Collections.emptyList()), null);
    }

    @Test
    void freeStyleProjectTestOneLabel() throws Exception {
        testFreeStyleProjectLabels(generateCauseDataWithLabels(Arrays.asList("test1")), "test1");
    }

    @Test
    void freeStyleProjectTestTwoLabels() throws Exception {
        testFreeStyleProjectLabels(
                generateCauseDataWithLabels(Arrays.asList("test1", "test2", "test with spaces")),
                "test1,test2,test with spaces");
    }

    private CauseDataBuilder generateCauseDataBase() {
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
                .withTargetProjectUrl("https://gitlab.org/test");
    }

    private CauseData generateCauseData() {
        return generateCauseDataBase().build();
    }

    private CauseData generateCauseDataWithLabels(List<String> labels) {
        return generateCauseDataBase().withMergeRequestLabels(labels).build();
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
