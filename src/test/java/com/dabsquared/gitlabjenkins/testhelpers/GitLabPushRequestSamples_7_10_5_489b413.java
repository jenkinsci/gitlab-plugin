package com.dabsquared.gitlabjenkins.testhelpers;

import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.PushEvent;

public class GitLabPushRequestSamples_7_10_5_489b413 implements GitLabPushRequestSamples {

    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";
    private static final String COMMIT_7A = "7a5db3baf5d42b4218a2a501c88f745559b1d24c";
    private static final String COMMIT_21 = "21d67fe28097b49a1a6fb5c82cbfe03d389e8685";
    private static final String COMMIT_9d = "9dbdd7a128a2789d0f436222ce116f1d8229e083";

    public PushEvent pushBrandNewMasterBranchRequest() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        EventProject project = new EventProject();
        project.setUrl("http://gitlabserver.example.com/project");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/master");
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter(COMMIT_7A);
        pushEvent.setProject(project);
        // no commit on new branches
        return pushEvent;
    }

    public PushEvent pushNewBranchRequest() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/test-new-branch1");
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter(COMMIT_7A);
        return pushEvent;
    }

    public PushEvent pushCommitRequest() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/test-new-branch1");
        pushEvent.setBefore(COMMIT_7A);
        pushEvent.setAfter(COMMIT_21);
        // pushEvent.setCommits(Collections.singletonList(commit().withId(COMMIT_21).build()));
        return pushEvent;
    }

    public PushEvent mergePushRequest() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/master");
        pushEvent.setBefore("ca84f96a846b0e241808ea7b75dfa3bf4cd3b98b");
        pushEvent.setAfter(COMMIT_9d);
        // pushEvent.setCommits(Arrays.asList(
        // commit().withId(COMMIT_21).build(),
        // commit().withId("c04c8822d1df397fb7e6dd3dd133018a0af567a8")
        //         .build(),
        // commit().withId(COMMIT_9d).build()));
        return pushEvent;
    }

    public PushEvent pushNewTagRequest() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/tags/test-tag-1");
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter(COMMIT_21);
        // pushEvent.setCommits(Collections.singletonList(commit().withId(COMMIT_21).build()));
        return pushEvent;
    }

    public PushEvent deleteBranchRequest() {
        PushEvent pushEvent = new PushEvent();
        pushEvent.setUserId(123L);
        pushEvent.setUserName("admin@example");
        pushEvent.setProjectId(345L);
        EventRepository repository = new EventRepository();
        repository.setName("test-repo");
        repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
        repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/test-branch-3-delete");
        pushEvent.setBefore("c34984ff6ed9935b3d843237947adbaaa85fc5f9");
        pushEvent.setAfter(ZERO_SHA);
        return pushEvent;
    }
}
