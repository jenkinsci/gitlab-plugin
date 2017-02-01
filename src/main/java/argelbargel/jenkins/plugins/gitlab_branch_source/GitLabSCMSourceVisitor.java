package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectSelector;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectVisibility;
import jenkins.scm.api.SCMSourceObserver;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.io.PrintStream;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;


class GitLabSCMSourceVisitor {
    private final SCMSourceObserver observer;
    private final GitLabSCMNavigator navigator;
    private final PrintStream log;

    GitLabSCMSourceVisitor(GitLabSCMNavigator navigator, SCMSourceObserver observer) {
        this.navigator = navigator;
        this.observer = observer;
        this.log = observer.getListener().getLogger();
    }

    void visitSources() throws IOException, InterruptedException {
        visitSources(navigator.getConnectionName(),
                GitLabProjectSelector.byId(navigator.getProjectSelectorId()),
                GitLabProjectVisibility.byId(navigator.getProjectVisibilityId()),
                navigator.getProjectSearchPattern());
    }

    private void visitSources(String connectionName, GitLabProjectSelector selector, GitLabProjectVisibility visibility, String searchPattern) throws InterruptedException, IOException {
        log("Looking up repositories for gitlab-connection %s...", navigator.getConnectionName());
        for (GitlabProject project : gitLabAPI(connectionName).findProjects(selector, visibility, searchPattern)) {
            checkInterrupt();
            visitProject(project);
        }
    }

    void visitProject(String sourceName) throws IOException, InterruptedException {
        GitlabProject project = gitLabAPI(navigator.getConnectionName()).getProject(sourceName);
        if (project != null) {
            visitProject(project);
        }
    }

    private void visitProject(GitlabProject project) throws IOException, InterruptedException {
        if (!observer.isObserving()) {
            return;
        }

        checkInterrupt();

        log("Proposing repository %s", project.getHttpUrl());
        SCMSourceObserver.ProjectObserver projectObserver = observer.observe(project.getPathWithNamespace());
        GitLabSCMSource source = new GitLabSCMSource(project, navigator.getSourceSettings());
        projectObserver.addSource(source);
        projectObserver.complete();
        source.afterSave();
    }

    private void log(String message, Object... data) {
        log.format(message + "\n", data);
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
