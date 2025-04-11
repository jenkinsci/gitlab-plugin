package com.dabsquared.gitlabjenkins.trigger.handler.push;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_10_5_489b413;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_7_5_1_36679b5;
import com.dabsquared.gitlabjenkins.testhelpers.GitLabPushRequestSamples_8_1_2_8c8af7b;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import hudson.plugins.git.UserRemoteConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class PushHookTriggerHandlerGitlabServerTest {

    static GitLabPushRequestSamples[] data() {
        return new GitLabPushRequestSamples[] {
            new GitLabPushRequestSamples_7_5_1_36679b5(),
            new GitLabPushRequestSamples_7_10_5_489b413(),
            new GitLabPushRequestSamples_8_1_2_8c8af7b()
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_pushBrandNewMasterBranchRequest(GitLabPushRequestSamples samples)
            throws Exception {
        PushHook hook = samples.pushBrandNewMasterBranchRequest();

        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_mergeRequestMergePushRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.mergePushRequest();

        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_pushCommitRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_pushNewBranchRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewBranchRequest();

        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_pushNewTagRequest(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewTagRequest();

        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void doNotCreateRevisionParameterAction_deleteBranchRequest(GitLabPushRequestSamples samples) {
        assertThrows(NoRevisionToBuildException.class, () -> {
            PushHook hook = samples.deleteBranchRequest();
            new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, null);
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction__deleteBranchRequest(GitLabPushRequestSamples samples) {
        assertThrows(NoRevisionToBuildException.class, () -> {
            PushHook hook = samples.deleteBranchRequest();
            RevisionParameterAction revisionParameterAction =
                    new PushHookTriggerHandlerImpl(true).createRevisionParameter(hook, null);
            assertThat(revisionParameterAction, is(notNullValue()));
            assertThat(revisionParameterAction.commit, is(hook.getAfter()));
            assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_pushCommitRequestWithGitScm(GitLabPushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        GitSCM gitSCM = new GitSCM("git@test.tld:test.git");
        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, gitSCM);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getRef().replaceFirst("^refs/heads", "remotes/origin")));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void createRevisionParameterAction_pushCommitRequestWith2Remotes(GitLabPushRequestSamples samples)
            throws Exception {
        PushHook hook = samples.pushCommitRequest();

        GitSCM gitSCM = new GitSCM(
                Arrays.asList(
                        new UserRemoteConfig("git@test.tld:test.git", null, null, null),
                        new UserRemoteConfig("git@test.tld:fork.git", "fork", null, null)),
                Collections.singletonList(new BranchSpec("")),
                false,
                Collections.emptyList(),
                null,
                null,
                null);
        RevisionParameterAction revisionParameterAction =
                new PushHookTriggerHandlerImpl(false).createRevisionParameter(hook, gitSCM);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<>()));
    }
}
