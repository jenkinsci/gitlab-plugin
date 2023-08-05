package com.dabsquared.gitlabjenkins.testhelpers;

import org.gitlab4j.api.webhook.EventRepository;
import org.gitlab4j.api.webhook.PushEvent;

public class GitLabPushRequestSamples_8_1_2_8c8af7b implements GitLabPushRequestSamples {

    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";
    private static final String COMMIT_25 = "258d6f6e21e6dda343f6e9f8e78c38f12bb81c87";
    private static final String COMMIT_63 = "63b30060be89f0338123f2d8975588e7d40a1874";
    private static final String COMMIT_64 = "64ed77c360ee7ac900c68292775bee2184c1e593";
    private static final String COMMIT_74 = "742d8d0b4b16792c38c6798b28ba1fa754da165e";
    private static final String COMMIT_E5 = "e5a46665b80965724b45fe921788105258b3ec5c";

    public PushEvent pushBrandNewMasterBranchRequest() {
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
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter(COMMIT_63);
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
        pushEvent.setAfter(COMMIT_25);
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
        pushEvent.setBefore(COMMIT_25);
        pushEvent.setAfter(COMMIT_74);
        // pushEvent.setCommits(Collections.singletonList(commit().withId(COMMIT_74).build()));
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
        pushEvent.setBefore("e8b9327c9704e308949f9d31dd0fae6abfac3798");
        pushEvent.setAfter(COMMIT_E5);
        // pushEvent.setCommits(Arrays.asList(
        //                 commit().withId(COMMIT_74).build(),
        //                 commit().withId("ab569fa9c51fa80d6509b277a6b587faf8e7cb72")
        //                         .build(),
        //                 commit().withId(COMMIT_E5).build()));
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
        pushEvent.setRepository(repository);
        pushEvent.setRef("refs/tags/test-tag-2");
        pushEvent.setBefore(ZERO_SHA);
        pushEvent.setAfter(COMMIT_64);
        // pushEvent.setCommits(Collections.singletonList(commit().withId(COMMIT_64).build()));
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
        pushEvent.setRef("refs/heads/test-branch-delete-1");
        pushEvent.setBefore("784c5ca7814aa7ea1913ae8e64187c31322946f0");
        pushEvent.setAfter(ZERO_SHA);
        return pushEvent;
    }
}
