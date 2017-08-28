package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.eclipse.jgit.transport.URIish;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public abstract class AbstractWebHookTriggerHandler<H extends WebHook> implements WebHookTriggerHandler<H> {

    private static final Logger LOGGER = Logger.getLogger(AbstractWebHookTriggerHandler.class.getName());

    @Override
    public void handle(Job<?, ?> job, H hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (ciSkip && isCiSkip(hook)) {
            LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
            return;
        }

        String targetBranch = getTargetBranch(hook);
        if (branchFilter.isBranchAllowed(targetBranch)) {
            LOGGER.log(Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));
            setCommitStatusPendingIfNecessary(job, hook);
            scheduleBuild(job, createActions(job, hook));
        } else {
            LOGGER.log(Level.INFO, "branch {0} is not allowed", targetBranch);
        }
    }

    protected abstract String getTriggerType();

    protected abstract boolean isCiSkip(H hook);

    private void setCommitStatusPendingIfNecessary(Job<?, ?> job, H hook) {
        if (job instanceof AbstractProject && ((AbstractProject) job).getPublishersList().get(GitLabCommitStatusPublisher.class) != null) {
            GitLabCommitStatusPublisher publisher =
                (GitLabCommitStatusPublisher) ((AbstractProject) job).getPublishersList().get(GitLabCommitStatusPublisher.class);
            GitLabClient client = job.getProperty(GitLabConnectionProperty.class).getClient();
            BuildStatusUpdate buildStatusUpdate = retrieveBuildStatusUpdate(hook);
            try {
                if (client == null) {
                    LOGGER.log(Level.SEVERE, "No GitLab connection configured");
                } else {
                    String targetUrl =
                        Jenkins.getInstance().getRootUrl() + job.getUrl() + job.getNextBuildNumber() + "/";
                    client.changeBuildStatus(buildStatusUpdate.getProjectId(), buildStatusUpdate.getSha(),
                        BuildState.pending, buildStatusUpdate.getRef(), publisher.getName(), targetUrl, null);
                }
            } catch (WebApplicationException | ProcessingException e) {
                LOGGER.log(Level.SEVERE, "Failed to set build state to pending", e);
            }
        }
    }

    private Action[] createActions(Job<?, ?> job, H hook) {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new CauseAction(new GitLabWebHookCause(retrieveCauseData(hook))));
        try {
            SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
            GitSCM gitSCM = getGitSCM(item);
            actions.add(createRevisionParameter(hook, gitSCM));
        } catch (NoRevisionToBuildException e) {
            LOGGER.log(Level.WARNING, "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                    new Object[]{hook, (job != null ? job.getFullName() : null)});
        }
        return actions.toArray(new Action[actions.size()]);
    }

    protected abstract CauseData retrieveCauseData(H hook);

    protected abstract String getTargetBranch(H hook);

    protected abstract RevisionParameterAction createRevisionParameter(H hook, GitSCM gitSCM) throws NoRevisionToBuildException;

    protected abstract BuildStatusUpdate retrieveBuildStatusUpdate(H hook);

    protected URIish retrieveUrIish(WebHook hook) {
        try {
            if (hook.getRepository() != null) {
                return new URIish(hook.getRepository().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    private void scheduleBuild(Job<?, ?> job, Action[] actions) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob) job;
            if (abstractProject.getQuietPeriod() > projectBuildDelay) {
                projectBuildDelay = abstractProject.getQuietPeriod();
            }
        }
        retrieveScheduleJob(job).scheduleBuild2(projectBuildDelay, actions);
    }

    private ParameterizedJobMixIn retrieveScheduleJob(final Job<?, ?> job) {
        // TODO 1.621+ use standard method
        return new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };
    }

    private GitSCM getGitSCM(SCMTriggerItem item) {
        if (item != null) {
            for (SCM scm : item.getSCMs()) {
                if (scm instanceof GitSCM) {
                    return (GitSCM) scm;
                }
            }
        }
        return null;
    }

    public static class BuildStatusUpdate {
        private final Integer projectId;
        private final String sha;
        private final String ref;

        @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
        public BuildStatusUpdate(Integer projectId, String sha, String ref) {
            this.projectId = projectId;
            this.sha = sha;
            this.ref = ref;
        }

        public Integer getProjectId() {
            return projectId;
        }

        public String getSha() {
            return sha;
        }

        public String getRef() {
            return ref;
        }
    }
}
