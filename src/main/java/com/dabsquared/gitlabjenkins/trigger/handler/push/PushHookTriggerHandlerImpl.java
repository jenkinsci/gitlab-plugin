package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Commit;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Job;
import org.eclipse.jgit.util.StringUtils;

import java.util.List;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import static com.dabsquared.gitlabjenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PushHook> implements PushHookTriggerHandler {

    private static final String NO_COMMIT = "0000000000000000000000000000000000000000";
    
    public PushHookTriggerHandlerImpl(boolean alwaysBuildHead) {
        super(alwaysBuildHead);
    }

    @Override
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isNoRemoveBranchPush(hook)) {
            super.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(PushHook hook) {
        List<Commit> commits = hook.getCommits();
        return commits != null &&
               !commits.isEmpty() &&
               commits.get(commits.size() - 1).getMessage() != null &&
               commits.get(commits.size() - 1).getMessage().contains("[ci-skip]");
    }

    @Override
    protected CauseData retrieveCauseData(PushHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.PUSH)
                .withSourceProjectId(hook.getProjectId())
                .withTargetProjectId(hook.getProjectId())
                .withBranch(getTargetBranch(hook))
                .withSourceBranch(getTargetBranch(hook))
                .withUserName(hook.getUserName())
                .withUserEmail(hook.getUserEmail())
                .withSourceRepoHomepage(hook.getRepository().getHomepage())
                .withSourceRepoName(hook.getRepository().getName())
                .withSourceNamespace(hook.getProject().getNamespace())
                .withSourceRepoUrl(hook.getRepository().getUrl())
                .withSourceRepoSshUrl(hook.getRepository().getGitSshUrl())
                .withSourceRepoHttpUrl(hook.getRepository().getGitHttpUrl())
                .withMergeRequestTitle("")
                .withMergeRequestDescription("")
                .withMergeRequestId(null)
                .withMergeRequestIid(null)
                .withTargetBranch(getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withTriggeredByUser(retrievePushedBy(hook))
                .withBefore(hook.getBefore())
                .withAfter(hook.getAfter())
                .withLastCommit(hook.getAfter())
                .withTargetProjectUrl(hook.getProject().getWebUrl())
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
    protected BuildStatusUpdate retrieveBuildStatusUpdate(PushHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getProjectId())
            .withSha(hook.getAfter())
            .withRef(getTargetBranch(hook))
            .build();
    }

    private String retrievePushedBy(final PushHook hook) {

        final String userName = hook.getUserName();
        if (!StringUtils.isEmptyOrNull(userName)) {
            return userName;
        }

        final List<Commit> commits = hook.getCommits();
        if (commits != null && !commits.isEmpty()) {
            return commits.get(commits.size() - 1).getAuthor().getName();
        }

        return null;
    }

    @Override
    protected String retrieveSourceBranch(PushHook hook) throws NoRevisionToBuildException {
        if (inNoBranchDelete(hook)) {
            //extract branch name 
            return hook.getRef().replaceFirst("^refs/heads/","");
        } else {
            throw new NoRevisionToBuildException();
        }            
    }
    
    protected String retrieveRevisionToBuild(PushHook hook) throws NoRevisionToBuildException {
        if (inNoBranchDelete(hook)) {
            return hook.getAfter();
        } else {
            throw new NoRevisionToBuildException();
        }
    }
    
    @Override
    protected String retrieveTargetBranch(PushHook hook) throws NoRevisionToBuildException {
        //when pushing, we don't integrate the branch
        return retrieveSourceBranch(hook);
    }
    
    private boolean inNoBranchDelete(PushHook hook) {
        return hook.getAfter() != null && !hook.getAfter().equals(NO_COMMIT);
    }

    private boolean isNoRemoveBranchPush(PushHook hook) {
        return hook.getAfter() != null && !hook.getAfter().equals(NO_COMMIT);
    }
}
