/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.trigger.handler.AbstractWebHookTriggerHandler;

/**
 *
 * @author Pablo Bendersky
 */
public abstract class AbstractMergeRequestHookTriggerHandler extends AbstractWebHookTriggerHandler<MergeRequestHook> {
    
    @Override
    protected CauseData retrieveCauseData(MergeRequestHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withProjectId(hook.getObjectAttributes().getTargetProjectId())
                .withBranch(hook.getObjectAttributes().getSourceBranch())
                .withSourceBranch(hook.getObjectAttributes().getSourceBranch())
                .withUserName(hook.getObjectAttributes().getLastCommit().getAuthor().getName())
                .withUserEmail(hook.getObjectAttributes().getLastCommit().getAuthor().getEmail())
                .withSourceRepoHomepage(hook.getObjectAttributes().getSource().getHomepage())
                .withSourceRepoName(hook.getObjectAttributes().getSource().getName())
                .withSourceNamespace(hook.getObjectAttributes().getSource().getNamespace())
                .withSourceRepoUrl(hook.getObjectAttributes().getSource().getUrl())
                .withSourceRepoSshUrl(hook.getObjectAttributes().getSource().getSshUrl())
                .withSourceRepoHttpUrl(hook.getObjectAttributes().getSource().getHttpUrl())
                .withMergeRequestTitle(hook.getObjectAttributes().getTitle())
                .withMergeRequestDescription(hook.getObjectAttributes().getDescription())
                .withMergeRequestId(hook.getObjectAttributes().getId())
                .withMergeRequestIid(hook.getObjectAttributes().getIid())
                .withTargetBranch(hook.getObjectAttributes().getTargetBranch())
                .withTargetRepoName(hook.getObjectAttributes().getTarget().getName())
                .withTargetNamespace(hook.getObjectAttributes().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getObjectAttributes().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getObjectAttributes().getTarget().getHttpUrl())
                .withTriggeredByUser(hook.getObjectAttributes().getLastCommit().getAuthor().getName())
                .build();
    }
    
}
