package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.gitlab.api.model.Commit;
import com.dabsquared.gitlabjenkins.gitlab.api.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.plugins.git.RevisionParameterAction;

import java.util.List;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PushHook> implements PushHookTriggerHandler {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected boolean isCiSkip(PushHook hook) {
        List<Commit> commits = hook.getCommits();
        return commits != null && !commits.isEmpty() && commits.get(0).getMessage() != null && commits.get(0).getMessage().contains("[ci-skip]");
    }

    @Override
    protected CauseData retrieveCauseData(PushHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.PUSH)
                .withProjectId(hook.getProjectId())
                .withBranch(getTargetBranch(hook))
                .withSourceBranch(getTargetBranch(hook))
                .withUserName(hook.getUserName())
                .withUserEmail(hook.getUserEmail())
                .withSourceRepoHomepage(hook.getRepository().getHomepage())
                .withSourceRepoName(hook.getRepository().getName())
                .withSourceRepoUrl(hook.getRepository().getUrl())
                .withSourceRepoSshUrl(hook.getRepository().getGitSshUrl())
                .withSourceRepoHttpUrl(hook.getRepository().getGitHttpUrl())
                .withMergeRequestTitle("")
                .withMergeRequestDescription("")
                .withMergeRequestId(null)
                .withTargetBranch(getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withTriggeredByUser(retrievePushedBy(hook))
                .build();
    }

    @Override
    protected String getTargetBranch(PushHook hook) {
        return hook.getRef() == null ? null : hook.getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "push";
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PushHook hook) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    private String retrievePushedBy(PushHook hook) {
        List<Commit> commits = hook.getCommits();
        if (commits != null && commits.size() > 0) {
            return commits.get(0).getAuthor().getName();
        } else {
            return hook.getUserName();
        }
    }

    private String retrieveRevisionToBuild(PushHook hook) throws NoRevisionToBuildException {
        if (hook.getCommits() == null || hook.getCommits().isEmpty()) {
            if (isNewBranchPush(hook)) {
                return hook.getAfter();
            } else {
                throw new NoRevisionToBuildException();
            }
        } else {
            List<Commit> commits = hook.getCommits();
            return commits.get(commits.size() - 1).getId();
        }
    }

    private boolean isNewBranchPush(PushHook pushHook) {
        return pushHook.getBefore() != null && pushHook.getBefore().contains("0000000000000000000000000000000000000000");
    }
}
