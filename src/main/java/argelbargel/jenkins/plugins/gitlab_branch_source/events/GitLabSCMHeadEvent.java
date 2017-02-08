package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import hudson.model.Cause;
import hudson.scm.SCM;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;

public abstract class GitLabSCMHeadEvent<T extends WebHook> extends SCMHeadEvent<T> implements GitLabSCMEvent {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMHeadEvent.class.getName());

    private final String hookId;

    GitLabSCMHeadEvent(Type type, String id, T payload) {
        super(type, payload);
        this.hookId = id;
    }

    @Override
    public final boolean isMatch(@Nonnull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    protected boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        return hookId.equals(navigator.getHookListener().id());
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
    public final Map<jenkins.scm.api.SCMHead, SCMRevision> heads(@Nonnull SCMSource source) {
        if (source instanceof GitLabSCMSource) {
            try {
                GitLabSCMHead head = head((GitLabSCMSource) source);
                return Collections.<jenkins.scm.api.SCMHead, SCMRevision>singletonMap(head, head.getRevision());
            } catch (Exception e) {
                LOGGER.warning("could not get heads from " + source + ": " + e.getMessage());
            }
        }

        return emptyMap();
    }

    @Override
    public final Cause getCause() {
        return new GitLabWebHookCause(getCauseData());
    }

    public abstract GitLabSCMHead head(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException;

    abstract CauseData getCauseData();

    @Override
    public boolean isMatch(@Nonnull SCM scm) {
        return false;
    }
}
