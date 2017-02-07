package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import hudson.model.Cause;

import javax.annotation.Nonnull;
import java.io.IOException;


public class GitLabSCMPushEvent extends GitLabSCMHeadEvent<PushHook> {
    public GitLabSCMPushEvent(String id, PushHook hook) {
        this(Type.UPDATED, id, hook);
    }

    GitLabSCMPushEvent(Type type, String id, PushHook hook) {
        super(type, id, hook);
    }

    @Nonnull
    @Override
    public final String getSourceName() {
        return getPayload().getProject().getPathWithNamespace();
    }

    @Override
    protected final boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        return super.isMatch(navigator) && EventHelper.getMatchingProject(navigator, getPayload()) != null;
    }

    @Override
    protected boolean isMatch(@Nonnull GitLabSCMSource source) {
        return super.isMatch(source) && getPayload().getProjectId().equals(source.getProjectId());
    }

    @Override
    public GitLabSCMHead head(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException {
        return source.createBranch(getPayload().getRef(), getPayload().getAfter());
    }

    @Override
    public final Cause getCause() {
        return new GitLabWebHookCause(CauseDataHelper.buildCauseData(getPayload()));
    }
}
