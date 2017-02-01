package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMCommit;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMException;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import hudson.model.Cause;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class GitLabSCMPushEvent extends SCMHeadEvent<PushHook> implements GitLabSCMEvent {
    private final String hookId;

    public GitLabSCMPushEvent(String id, PushHook hook) {
        super(Type.UPDATED, hook);
        this.hookId = id;
    }

    @Override
    public boolean isMatch(@Nonnull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    private boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        // Pushes to unknown projects trigger update if accessible by navigator
        return hookId.equals(navigator.getHookListener().id()) && EventHelper.getMatchingProject(navigator, getPayload()) != null;
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
        if (!hookId.equals(source.getHookListener().id())) {
            return emptyMap();
        }

        Map<SCMHead, SCMRevision> result = new HashMap<>();
        try {
            for (SCMHead head : source.fetch(TaskListener.NULL)) {
                result.put(head, new GitLabSCMCommit(head, getPayload().getAfter()));
            }
        } catch (InterruptedException e) {
            // silently ignore
        } catch (IOException e) {
            throw new GitLabSCMException("error fetching heads for source + " + source, e);
        }

        return result;
    }

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }

    public Cause getCause() {
        return new GitLabWebHookCause(CauseDataHelper.buildCauseData(getPayload()));
    }
}
