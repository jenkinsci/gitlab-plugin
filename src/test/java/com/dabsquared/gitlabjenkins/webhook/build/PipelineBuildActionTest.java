package com.dabsquared.gitlabjenkins.webhook.build;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.ibm.icu.text.SimpleDateFormat;

import hudson.model.FreeStyleProject;
import javassist.Loader.Simple;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.m;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.models.Visibility;
import org.gitlab4j.api.webhook.BuildEvent;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.PipelineEvent;
import org.gitlab4j.api.webhook.PipelineEvent.ObjectAttributes;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Milena Zachow
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineBuildActionTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private StaplerResponse response;

    @Mock
    private GitLabPushTrigger trigger;

    FreeStyleProject testProject;
    private PipelineEvent pipelineEvent;

    @Before
    public void setUp() throws IOException, ParseException {
        testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        pipelineEvent.setObjectKind("pipeline");
        ObjectAttributes objectAttributes = new ObjectAttributes();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        objectAttributes.setId(31L);
        objectAttributes.setRef("master");
        objectAttributes.setTag(false);
        objectAttributes.setSha("bcbb5ec396a2c0f828686f14fac9b80b780504f2");
        objectAttributes.setBeforeSha("bcbb5ec396a2c0f828686f14fac9b80b780504f2");
        objectAttributes.setStatus("success");
        objectAttributes.setStages(Arrays.asList("build", "test", "deploy"));
        objectAttributes.setCreatedAt(dateFormat.parse("2016-08-11 11:28:34 UTC"));
        objectAttributes.setFinishedAt(dateFormat.parse("2016-08-12 15:26:29 UTC"));
        User user = new User();
        user.setName("Administrator");
        user.setUsername("root");
        user.setAvatarUrl("http://www.gravatar.com/avatar/e32bd13e2add097461cb96824b7a829c?s=80\u0026d=identicon");
        EventProject project = new EventProject();
        project.setId(1L);
        project.setName("GitLab Test");
        project.setDescription("Atque in sunt eos similique dolores voluptatem.");
        project.setWebUrl("http://192.168.64.1:3005/gitlab-org/gitlab-test");
        project.setAvatarUrl(null);
        project.setGitSshUrl("git@192.168.64.1:gitlab-org/gitlab-test.git");
        project.setGitHttpUrl("http://192.168.64.1:3005/gitlab-org/gitlab-test.git");
        project.setNamespace("Gitlab Org");
        //project.setVisibilityLevel(Visibility.PUBLIC); 
        project.setPathWithNamespace("gitlab-org/gitlab-test");
        project.setDefaultBranch("master");
        EventCommit commit = new EventCommit();
        commit.setId("bcbb5ec396a2c0f828686f14fac9b80b780504f2");
        commit.setMessage("test\n");
        commit.setTimestamp(dateFormat.parse("2016-08-11 11:28:34 UTC"));
        commit.setUrl("http://example.com/gitlab-org/gitlab-test/commit/bcbb5ec396a2c0f828686f14fac9b80b780504f2");
        Author author = new Author();
        author.setName("User");
        author.setEmail("user@gitlab.com");
        commit.setAuthor(author);
        BuildEvent buildEvent1 = new BuildEvent();
        buildEvent1.setBuildId(380L);
        buildEvent1.setBuildStage("deploy");
        buildEvent1.setBuildName("production");
        buildEvent1.setBuildStatus("skipped");
        // createdat is missing in buildevent
        // buildEvent1.setCreatedAt(dateFormat.parse("2016-08-12 15:26:29 UTC")); 
        buildEvent1.setBuildStarted_at(null);
        buildEvent1.setBuildFinished_at(null);
        buildEvent1.setUser(user);
        // when, user, runner, artifact_file not found in buildEvent
        BuildEvent buildEvent2 = new BuildEvent();
        buildEvent2.setBuildId(377L);
        buildEvent2.setBuildStage("test");
        buildEvent2.setBuildName("test-image");
        buildEvent2.setBuildStatus("success");
        // buildEvent2.setCreatedAt(dateFormat.parse("2016-08-12 15:23:28 UTC")); 
        buildEvent2.setBuildStarted_at(dateFormat.parse("2016-08-12 15:26:12 UTC"));
        buildEvent2.setBuildFinished_at(null);
        buildEvent2.setUser(user);
        BuildEvent buildEvent3 = new BuildEvent();
        buildEvent3.setBuildId(378L);
        buildEvent3.setBuildStage("test");
        buildEvent3.setBuildName("test-build");
        buildEvent3.setBuildStatus("success");
        // buildEvent3.setCreatedAt("2016-08-12 15:23:28 UTC"); 
        buildEvent3.setBuildStarted_at(dateFormat.parse("2016-08-12 15:26:12 UTC"));
        buildEvent3.setBuildFinished_at(dateFormat.parse("2016-08-12 15:26:29 UTC"));
        buildEvent3.setUser(user);
        BuildEvent buildEvent4 = new BuildEvent();
        buildEvent4.setBuildId(376L);
        buildEvent4.setBuildStage("build");
        buildEvent4.setBuildName("build-image");
        buildEvent4.setBuildStatus("success");
        // buildEvent4.setCreatedAt(dateFormat.parse("2016-08-12 15:23:28 UTC")); 
        buildEvent4.setBuildStarted_at(dateFormat.parse("2016-08-12 15:24:56 UTC"));
        buildEvent4.setBuildFinished_at(dateFormat.parse("2016-08-12 15:25:26 UTC"));
        buildEvent4.setUser(user);
        BuildEvent buildEvent5 = new BuildEvent();
        buildEvent5.setBuildId(379L);
        buildEvent5.setBuildStage("deploy");
        buildEvent5.setBuildName("staging");
        buildEvent5.setBuildStatus("created");
        // buildEvent5.setCreatedAt(dateFormat.parse("2016-08-12 15:23:28 UTC")); 
        buildEvent5.setBuildStarted_at(null);
        buildEvent5.setBuildFinished_at(null);
        buildEvent5.setUser(user);
        pipelineEvent.setCommit(commit);
        pipelineEvent.setProject(project);
        pipelineEvent.setUser(user);
        pipelineEvent.setObjectAttributes(objectAttributes);
    }

    @Test
    public void buildOnSuccess() throws IOException {
        exception.expect(HttpResponses.HttpResponseException.class);
        new PipelineBuildAction(testProject, pipelineEvent, null).execute(response);

        verify(trigger).onPost(any(PipelineEvent.class));
    }

    @Test
    public void doNotBuildOnFailure() throws IOException {
        exception.expect(HttpResponses.HttpResponseException.class);
        PipelineEvent pipelineFailureEvent = pipelineEvent;
        pipelineFailureEvent.getObjectAttributes().setStatus("failed");
        new PipelineBuildAction(testProject, pipelineFailureEvent, null).execute(response);

        verify(trigger, never()).onPost(any(PipelineEvent.class));
    }
}
