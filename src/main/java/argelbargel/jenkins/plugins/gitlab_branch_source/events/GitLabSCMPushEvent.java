package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import hudson.model.Cause;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;


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
    protected Map<SCMHead, SCMRevision> heads(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException {
        String ref = getPayload().getRef();
        GitLabSCMHead head = source.createBranch(ref, getPayload().getAfter());
        return Collections.<SCMHead, SCMRevision>singletonMap(head, head.getRevision());
    }


    @Override
    public final Cause getCause() {
        return new GitLabWebHookCause(CauseDataHelper.buildCauseData(getPayload()));
    }
}
