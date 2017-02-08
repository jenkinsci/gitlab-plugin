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
import java.util.Objects;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.REVISION_HEAD;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createMergeRequest;
import static argelbargel.jenkins.plugins.gitlab_branch_source.events.CauseDataHelper.buildCauseData;
import static jenkins.scm.api.SCMEvent.Type.CREATED;
import static jenkins.scm.api.SCMEvent.Type.REMOVED;
import static jenkins.scm.api.SCMEvent.Type.UPDATED;


public final class GitLabSCMMergeRequestEvent extends GitLabSCMHeadEvent<MergeRequestHook> {
    public static GitLabSCMMergeRequestEvent create(String id, MergeRequestHook hook) {
        switch (hook.getObjectAttributes().getAction()) {
            case open:
                return new GitLabSCMMergeRequestEvent(CREATED, id, hook);
            case update:
                return new GitLabSCMMergeRequestEvent(UPDATED, id, hook);
            default:
                // other actions are "merged" and "closed". in both cases we can remove the head
                return new GitLabSCMMergeRequestEvent(REMOVED, id, hook);
        }
    }

    private GitLabSCMMergeRequestEvent(Type type, String id, MergeRequestHook payload) {
        super(type, id, payload);
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
    Collection<GitLabSCMHead> heads(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException {
        Collection<GitLabSCMHead> heads = new ArrayList<>();

        GitLabSCMMergeRequestHead head = createMergeRequest(
                getAttributes().getId(), getAttributes().getTitle(),
                GitLabSCMHead.createBranch(getAttributes().getSourceBranch(), getAttributes().getLastCommit().getId()),
                GitLabSCMHead.createBranch(getAttributes().getTargetBranch(), REVISION_HEAD));

        boolean fromOrigin = Objects.equals(getAttributes().getSourceProjectId(), source.getProjectId());

        if ((fromOrigin && source.getBuildMergeRequestsFromOriginUnmerged()) || (!fromOrigin && source.getBuildMergeRequestsFromForksUnmerged())) {
            heads.add(head);
        }

        if ((fromOrigin && source.getBuildMergeRequestsFromOriginMerged()) || (!fromOrigin && source.getBuildMergeRequestsFromForksMerged())) {
            heads.add(head.merged());
        }

        return heads;
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return getAttributes().getTarget().getPathWithNamespace();
    }
}
