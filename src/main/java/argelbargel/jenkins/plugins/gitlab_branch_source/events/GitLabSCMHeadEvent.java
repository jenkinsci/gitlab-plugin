package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import hudson.model.Cause;
import hudson.scm.SCM;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;

public abstract class GitLabSCMHeadEvent extends SCMHeadEvent<PushHook> implements GitLabSCMEvent {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMHeadEvent.class.getName());

    private final String hookId;

    GitLabSCMHeadEvent(Type type, PushHook payload, String id) {
        super(type, payload);
        this.hookId = id;
    }

    @Override
    public final boolean isMatch(@Nonnull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    private boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        return hookId.equals(navigator.getHookListener().id()) && EventHelper.getMatchingProject(navigator, getPayload()) != null;
    }

    @Override
    public final boolean isMatch(@Nonnull SCMSource source) {
        return source instanceof GitLabSCMSource && isMatch((GitLabSCMSource) source);
    }

    protected boolean isMatch(@Nonnull GitLabSCMSource source) {
        return hookId.equals(source.getHookListener().id()) && getPayload().getProjectId().equals(source.getProjectId());
    }

    @Nonnull
    @Override
    public final String getSourceName() {
        return getPayload().getProject().getPathWithNamespace();
    }

    @Nonnull
    @Override
    public final Map<SCMHead, SCMRevision> heads(@Nonnull SCMSource source) {
        if (source instanceof GitLabSCMSource) {
            try {
                return heads((GitLabSCMSource) source);
            } catch (Exception e) {
                LOGGER.warning("could not get heads from " + source + ": " + e.getMessage());
            }
        }

        return emptyMap();
    }

    protected abstract Map<SCMHead, SCMRevision> heads(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException;

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }

    @Override
    public final Cause getCause() {
        return new GitLabWebHookCause(CauseDataHelper.buildCauseData(getPayload()));
    }
}
