package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import hudson.model.Cause;
import hudson.scm.SCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import javax.annotation.Nonnull;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class GitLabSCMTagPushEvent extends GitLabSCMPushEvent implements GitLabSCMEvent {
    public GitLabSCMTagPushEvent(String id, PushHook hook) {
        super(id, hook);
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return getPayload().getProject().getPathWithNamespace();
    }

    @Nonnull
    @Override
    public Map<SCMHead, SCMRevision> heads(@Nonnull SCMSource source) {
        if (source instanceof GitLabSCMSource) {
            return heads((GitLabSCMSource) source);
        }

        return emptyMap();
    }

    protected Map<SCMHead, SCMRevision> heads(@Nonnull GitLabSCMSource source) {
        if (!source.getBuildTags()) {
            return emptyMap();
        }

        return super.heads(source);
    }

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }

    @Override
    public Cause getCause() {
        return new GitLabWebHookCause(CauseDataHelper.buildCauseData(getPayload()));
    }
}
