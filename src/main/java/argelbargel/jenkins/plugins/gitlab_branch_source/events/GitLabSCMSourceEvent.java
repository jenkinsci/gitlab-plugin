package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.SystemHook;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceEvent;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.Nonnull;

public final class GitLabSCMSourceEvent extends SCMSourceEvent<SystemHook> {
    public static GitLabSCMSourceEvent create(String id, SystemHook hook, String origin) {
        if (hook.isProjectCreated()) {
            return new GitLabSCMSourceEvent(Type.CREATED, id, hook, origin);
        }

        if (hook.isProjectDestroyed()) {
            return new GitLabSCMSourceEvent(Type.UPDATED, id, hook, origin);
        }

        if (hook.isProjectUpdated()) {
            return new GitLabSCMSourceEvent(Type.UPDATED, id, hook, origin);
        }

        throw new IllegalArgumentException("cannot handle system-hook " + hook);
    }

    private final String hookId;
    private String sourceName;

    private GitLabSCMSourceEvent(@Nonnull Type type, @Nonnull String id, @Nonnull SystemHook hook, String origin) {
        super(type, hook, origin);
        hookId = id;
        sourceName = getPayload().getPathWithNamespace();
    }

    @Override
    public boolean isMatch(@Nonnull SCMNavigator navigator) {
        return navigator instanceof GitLabSCMNavigator && isMatch((GitLabSCMNavigator) navigator);
    }

    private boolean isMatch(@Nonnull GitLabSCMNavigator navigator) {
        if (!navigator.getHookListener().id().equals(hookId)) {
            return false;
        }

        switch (getType()) {
            case CREATED:
            case UPDATED:
                GitlabProject project = EventHelper.getMatchingProject(navigator, getPayload());
                if (project == null) {
                    return false;
                }

                sourceName = project.getPathWithNamespace();
                return true;
            case REMOVED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isMatch(@Nonnull SCMSource source) {
        return (source instanceof GitLabSCMSource) && isMatch((GitLabSCMSource) source);
    }

    private boolean isMatch(@Nonnull GitLabSCMSource source) {
        if (!source.getHookListener().id().startsWith(hookId) || source.getProjectId() != (getPayload().getProjectId())) {
            return false;
        }

        sourceName = source.getId();
        return true;
    }

    @Nonnull
    @Override
    public String getSourceName() {
        return sourceName;
    }
}
