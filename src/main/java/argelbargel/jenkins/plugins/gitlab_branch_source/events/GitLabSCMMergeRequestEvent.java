package argelbargel.jenkins.plugins.gitlab_branch_source.events;


import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMMergeRequestHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.REVISION_HEAD;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createBranch;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createMergeRequest;
import static argelbargel.jenkins.plugins.gitlab_branch_source.events.CauseDataHelper.buildCauseData;
import static jenkins.scm.api.SCMEvent.Type.CREATED;
import static jenkins.scm.api.SCMEvent.Type.REMOVED;
import static jenkins.scm.api.SCMEvent.Type.UPDATED;


public final class GitLabSCMMergeRequestEvent extends GitLabSCMHeadEvent<MergeRequestHook> {
    public static GitLabSCMMergeRequestEvent create(String id, MergeRequestHook hook, String origin) {
        switch (hook.getObjectAttributes().getAction()) {
            case open:
                return new GitLabSCMMergeRequestEvent(CREATED, id, hook, origin);
            case update:
                return new GitLabSCMMergeRequestEvent(UPDATED, id, hook, origin);
            default:
                // other actions are "merged" and "closed". in both cases we can remove the head
                return new GitLabSCMMergeRequestEvent(REMOVED, id, hook, origin);
        }
    }

    private GitLabSCMMergeRequestEvent(Type type, String id, MergeRequestHook payload, String origin) {
        super(type, id, payload, origin);
    }

    @Override
    CauseData getCauseData() {
        return buildCauseData(getPayload());
    }

    @Override
    protected boolean isMatch(@Nonnull GitLabSCMSource source) {
        if (!super.isMatch(source) || !isOrigin(source, getAttributes().getTargetProjectId())) {
            return false;
        }

        boolean isOrigin = isOrigin(source, getAttributes().getSourceProjectId());
        return ((isOrigin && source.getMonitorAndBuildMergeRequestsFromOrigin()) || (!isOrigin && source.getMonitorAndBuildMergeRequestsFromForks()));
    }

    private boolean isOrigin(@Nonnull GitLabSCMSource source, Integer projectId) {
        return projectId.equals(source.getProjectId());
    }

    private MergeRequestObjectAttributes getAttributes() {
        return getPayload().getObjectAttributes();
    }

    @Override
    Collection<? extends GitLabSCMHead> heads(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException {
        Collection<GitLabSCMHead> heads = new ArrayList<>();

        MergeRequestObjectAttributes attributes = getAttributes();
        Integer sourceProjectId = attributes.getSourceProjectId();
        String sourceBranch = attributes.getSourceBranch();
        String hash = attributes.getLastCommit().getId();
        GitLabSCMMergeRequestHead head = createMergeRequest(
                attributes.getId(), attributes.getTitle(),
                createBranch(sourceProjectId, sourceBranch, hash),
                createBranch(attributes.getTargetProjectId(), attributes.getTargetBranch(), REVISION_HEAD));

        if (source.buildUnmerged(head)) {
            heads.add(head);
        }

        if (source.buildMerged(head)) {
            heads.add(head.merged());
        }

        if (head.fromOrigin()) {
            heads.add(createBranch(sourceProjectId, sourceBranch, hash));
        }

        return heads;
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return getAttributes().getTarget().getPathWithNamespace();
    }
}
