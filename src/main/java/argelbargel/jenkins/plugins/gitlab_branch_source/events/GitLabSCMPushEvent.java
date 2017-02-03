package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
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
import java.util.Collections;
import java.util.Map;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHeads.createBranch;
import static java.util.Collections.emptyMap;


public class GitLabSCMPushEvent extends SCMHeadEvent<PushHook> implements GitLabSCMEvent {
    private final String hookId;

    public GitLabSCMPushEvent(String id, PushHook hook) {
        this(Type.UPDATED, id, hook);
    }

    GitLabSCMPushEvent(Type type, String id, PushHook hook) {
        super(type, hook);
        this.hookId = id;
    }

    @Override
    public final boolean isMatch(@Nonnull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    private boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        // Pushes to unknown projects trigger update if accessible by navigator
        return hookId.equals(navigator.getHookListener().id()) && EventHelper.getMatchingProject(navigator, getPayload()) != null;
    }

    @Override
    public final boolean isMatch(@Nonnull SCMSource source) {
        return source instanceof GitLabSCMSource && isMatch((GitLabSCMSource) source);
    }

    protected boolean isMatch(@Nonnull GitLabSCMSource source) {
        return hookId.equals(source.getHookListener().id());
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
            return heads((GitLabSCMSource) source);
        }

        return emptyMap();
    }

    protected Map<SCMHead, SCMRevision> heads(@Nonnull GitLabSCMSource source) {
        GitLabSCMHead head = createBranch(getPayload().getRef(), getPayload().getAfter());
        return Collections.<SCMHead, SCMRevision>singletonMap(head, head.getCommit());
    }

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }

    @Override
    public final Cause getCause() {
        return new GitLabWebHookCause(CauseDataHelper.buildCauseData(getPayload()));
    }
}
