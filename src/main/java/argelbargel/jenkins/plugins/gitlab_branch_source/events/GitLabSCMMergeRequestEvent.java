package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import hudson.model.Cause;

import javax.annotation.Nonnull;
import java.io.IOException;

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
    public Cause getCause() {
        return null;
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
    public GitLabSCMHead head(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException {
        return source.createMergeRequest(
                getAttributes().getId(), getAttributes().getTitle(),
                getAttributes().getSourceBranch(),
                getAttributes().getLastCommit().getId(),
                getAttributes().getTargetBranch());
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return getAttributes().getTarget().getPathWithNamespace();
    }
}
