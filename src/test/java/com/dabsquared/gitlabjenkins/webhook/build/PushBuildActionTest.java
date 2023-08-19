package com.dabsquared.gitlabjenkins.webhook.build;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jenkins.plugins.git.GitSCMSource;
import jenkins.plugins.git.traits.IgnoreOnPushNotificationTrait;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.io.IOUtils;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.webhook.EventCommit;
import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.PushEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public class PushBuildActionTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private StaplerResponse response;

    @Mock
    private GitLabPushTrigger trigger;

    private PushEvent pushEvent;

    @Before
    public void setUp() throws ParseException {
        pushEvent = new PushEvent();
        pushEvent.setObjectKind("push");
        pushEvent.setBefore("95790bf891e76fee5e1747ab589903a6a1f80f22");
        pushEvent.setAfter("da1560886d4f094c3e6c9ef40349f7d38b5d27d7");
        pushEvent.setRef("refs/heads/master");
        pushEvent.setUserId(4L);
        pushEvent.setUserName("John Smith");
        pushEvent.setUserUsername("jsmith");
        pushEvent.setUserEmail("john@example.com");
        pushEvent.setUserAvatar(
                "https://s.gravatar.com/avatar/d4c74594d841139328695756648b6bd6?s=8://s.gravatar.com/avatar/d4c74594d841139328695756648b6bd6?s=80");
        pushEvent.setProjectId(15L);
        EventProject project = new EventProject();
        project.setName("Diaspora");
        project.setDescription("");
        project.setWebUrl("http://example.com/mike/diaspora");
        project.setAvatarUrl(null);
        project.setGitSshUrl("git@example.com:mike/diaspora.git");
        project.setGitHttpUrl("http://example.com/mike/diaspora.git");
        project.setNamespace("Mike");
        project.setVisibilityLevel(AccessLevel.NONE);
        project.setPathWithNamespace("mike/diaspora");
        project.setDefaultBranch("master");
        project.setHomepage("http://example.com/mike/diaspora");
        project.setUrl("git@example.com:mike/diasporadiaspora.git");
        project.setSshUrl("git@example.com:mike/diaspora.git");
        project.setHttpUrl("http://example.com/mike/diaspora.git");
        pushEvent.setProject(project);
        EventRepository repository = new EventRepository();
        repository.setName("Diaspora");
        repository.setUrl("git@example.com:mike/diasporadiaspora.git");
        repository.setDescription("");
        repository.setHomepage("http://example.com/mike/diaspora");
        repository.setGit_ssh_url("http://example.com/mike/diaspora.git");
        repository.setGit_ssh_url("http://example.com/mike/diaspora.git");
        repository.setVisibility_level(AccessLevel.NONE);
        pushEvent.setRepository(repository);
        EventCommit commit1 = new EventCommit();
        commit1.setId("b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327");
        commit1.setMessage("Update Catalan translation to e38cb41.");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        Date date1 = dateFormat.parse("2011-12-12T14:27:31+02:00");
        commit1.setTimestamp(date1);
        commit1.setUrl("http://example.com/mike/diaspora/commit/b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327");
        Author author = new Author();
        author.setName("Jordi Mallach");
        author.setEmail("jordi@softcatala.org");
        commit1.setAuthor(author);
        List<String> added = Arrays.asList("CHANGELOG");
        commit1.setAdded(added);
        List<String> modified = Arrays.asList("app/controller/application.rb");
        commit1.setModified(modified);
        List<String> removed = Arrays.asList();
        commit1.setRemoved(removed);
        EventCommit commit2 = new EventCommit();
        commit2.setId("da1560886d4f094c3e6c9ef40349f7d38b5d27d7");
        commit2.setMessage("fixed readme");
        Date date2 = dateFormat.parse("2012-01-03T23:36:29+02:00");
        commit2.setTimestamp(date2);
        commit2.setUrl("http://example.com/mike/diaspora/commit/da1560886d4f094c3e6c9ef40349f7d38b5d27d7");
        Author author2 = new Author();
        author2.setName("GitLab dev user");
        author2.setEmail("gitlabdev@dv6700.(none)");
        commit2.setAuthor(author2);
        commit2.setAdded(added);
        commit2.setModified(modified);
        commit2.setRemoved(removed);
        pushEvent.setTotalCommitsCount(4);
        List<EventCommit> commits = Arrays.asList(commit1, commit2);
        pushEvent.setCommits(commits);
    }

    @Test
    public void skip_missingRepositoryUrl() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        PushEvent pushEvent_missingRepositoryUrl = pushEvent;
        pushEvent_missingRepositoryUrl.setUserUsername(null);
        pushEvent_missingRepositoryUrl.getRepository().setUrl(null);
        new PushBuildAction(testProject, pushEvent_missingRepositoryUrl, null).execute(response);

        verify(trigger, never()).onPost(any(PushEvent.class));
    }

    @Test
    public void build() throws IOException {
        try {
            FreeStyleProject testProject = jenkins.createFreeStyleProject();
            when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn(TriggerOpenMergeRequest.never);
            testProject.addTrigger(trigger);

            exception.expect(HttpResponses.HttpResponseException.class);
            new PushBuildAction(testProject, pushEvent, null).execute(response);
        } finally {
            ArgumentCaptor<PushEvent> pushHookArgumentCaptor = ArgumentCaptor.forClass(PushEvent.class);
            verify(trigger).onPost(pushHookArgumentCaptor.capture());
            assertThat(pushHookArgumentCaptor.getValue().getProject(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getProject().getWebUrl(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getUserUsername(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getUserUsername(), containsString("jsmith"));
        }
    }

    @Test
    public void invalidToken() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn(TriggerOpenMergeRequest.never);
        when(trigger.getSecretToken()).thenReturn("secret");
        testProject.addTrigger(trigger);

        exception.expect(HttpResponses.HttpResponseException.class);
        new PushBuildAction(testProject, pushEvent, "wrong-secret").execute(response);

        verify(trigger, never()).onPost(any(PushEvent.class));
    }

    private String getJson(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name));
    }

    @Test
    public void scmSourceOnUpdateExecuted() {
        GitSCMSource source = new GitSCMSource("http://test");
        SCMSourceOwner item = mock(SCMSourceOwner.class);
        when(item.getSCMSources()).thenReturn(Collections.singletonList(source));
        Assert.assertThrows(HttpResponses.HttpResponseException.class, () -> new PushBuildAction(item, pushEvent, null)
                .execute(response));
        verify(item).onSCMSourceUpdated(isA(GitSCMSource.class));
    }

    @Test
    public void scmSourceOnUpdateNotExecuted() {
        GitSCMSource source = new GitSCMSource("http://test");
        source.getTraits().add(new IgnoreOnPushNotificationTrait());
        SCMSourceOwner item = mock(SCMSourceOwner.class);
        when(item.getSCMSources()).thenReturn(Collections.singletonList(source));
        Assert.assertThrows(HttpResponses.HttpResponseException.class, () -> new PushBuildAction(item, pushEvent, null)
                .execute(response));
        verify(item, never()).onSCMSourceUpdated(isA(GitSCMSource.class));
    }
}
