package com.dabsquared.gitlabjenkins.testhelpers;

import org.gitlab4j.api.webhook.EventProject;
import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.PushEvent;

public class GitLabPushRequestSamples_7_5_1_36679b5 implements GitLabPushRequestSamples {

    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";

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
        pushEvent.setAfter("d91a0f248625f6dc808fb7cda75c4ee01516b609");
        pushEvent.setProject(project);
        // no checkout_sha and no commit on new branches
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
        EventProject project = new EventProject();
        project.setUrl("http://gitlabserver.example.com/project");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/test-new-branch1");
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter("2bf4170829aedd706d7485d40091a01637b9abf4");
        pushEvent.setProject(project);
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
        EventProject project = new EventProject();
        project.setUrl("http://gitlabserver.example.com/project");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/test-new-branch1");
        pushEvent.setBefore("2bf4170829aedd706d7485d40091a01637b9abf4");
        pushEvent.setAfter("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb");
        pushEvent.setProject(project);
        // no checkout_sha
        // pushEvent.setCommits(Collections.singletonList(commit().withId("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb"));
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
        EventProject project = new EventProject();
        project.setUrl("http://gitlabserver.example.com/project");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/master");
        pushEvent.setBefore("27548e742f40971f75c715aaa7920404eeff6616");
        pushEvent.setAfter("3ebb6927ad4afbe8a11830938b3584cdaf4d657b");
        pushEvent.setProject(project);
        // no checkout_sha
        // pushEvent.setCommits(Arrays.asList(
        // commit().withId("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb")
        //         .build(),
        // commit().withId("be473fcc670b920cc9795581a5cd8f00fa7afddd")
        //         .build(),
        // commit().withId("3ebb6927ad4afbe8a11830938b3584cdaf4d657b")
        //         .build()));
        return pushEvent;
        // and afterwards the "delete branch" request comes in
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
        EventProject project = new EventProject();
        project.setUrl("http://gitlabserver.example.com/project");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/tags/test-tag-2");
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter("f10d9d7b648e5a3e55fe8fe865aba5aa7404df7c");
        pushEvent.setProject(project);
        // no checkout_sha and no commit on new branches
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
        EventProject project = new EventProject();
        project.setUrl("http://gitlabserver.example.com/project");
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/heads/test-branch-delete-1");
        pushEvent.setBefore("3ebb6927ad4afbe8a11830938b3584cdaf4d657b");
        pushEvent.setAfter(ZERO_SHA);
        pushEvent.setProject(project);
        // no checkout_sha and no commit on new branches
        return pushEvent;
    }
}
