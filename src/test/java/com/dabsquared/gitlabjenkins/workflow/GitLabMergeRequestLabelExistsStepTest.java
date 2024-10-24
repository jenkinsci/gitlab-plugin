package com.dabsquared.gitlabjenkins.workflow;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.CauseDataBuilder;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import hudson.model.CauseAction;
import hudson.model.Descriptor.FormException;
import hudson.model.queue.QueueTaskFuture;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GitLabMergeRequestLabelExistsStepTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private void test_webhook_base(CauseData causeData, String expected_log_msg)
            throws IOException, ExecutionException, InterruptedException, FormException {
        // load the pipeline script from resources
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        String pipelineText = IOUtils.toString(
                getClass().getResourceAsStream("jenkinsFile/GitLabMergeRequestLabel-jenkinsFile.groovy"),
                StandardCharsets.UTF_8);
        project.setDefinition(new CpsFlowDefinition(pipelineText, false));

        // create a merge request webhook and schedule it
        GitLabWebHookCause cause = new GitLabWebHookCause(causeData);
        QueueTaskFuture<?> future = project.scheduleBuild2(0, new CauseAction(cause));

        // wait for the build to finish and obtain the run object
        WorkflowRun run = (WorkflowRun) future.get();

        // observe the logs to see if the label was detected or not
        jenkins.assertLogContains(expected_log_msg, run);
    }

    @Test
    public void test_label_exists_in_mr_webhook()
            throws IOException, ExecutionException, InterruptedException, FormException {
        // create a cause data object with a label
        CauseData causeData = generateCauseDataWithLabels(Arrays.asList("test label"));
        test_webhook_base(causeData, "test label found");
    }

    @Test
    public void test_label_doesnt_exist_in_mr_webhook()
            throws IOException, ExecutionException, InterruptedException, FormException {
        // create a cause data object with a label
        CauseData causeData = generateCauseData();
        test_webhook_base(causeData, "test label not found");
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
}
