package com.dabsquared.gitlabjenkins.trigger.handler;

import com.dabsquared.gitlabjenkins.action.BranchQueueAction;
import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.trigger.exception.NoRevisionToBuildException;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilter;
import com.dabsquared.gitlabjenkins.util.LoggerUtil;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import hudson.scm.SCM;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;

/**
 * @author Robin Müller
 */
public abstract class AbstractWebHookTriggerHandler<H extends WebHook> implements WebHookTriggerHandler<H> {

    private static final Logger LOGGER = Logger.getLogger(AbstractWebHookTriggerHandler.class.getName());
    protected PendingBuildsHandler pendingBuildsHandler = new PendingBuildsHandler();

    @Override
    public void handle(
            Job<?, ?> job,
            H hook,
            boolean ciSkip,
            BranchFilter branchFilter,
            MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (ciSkip && isCiSkip(hook)) {
            LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
            return;
        }

        String sourceBranch = getSourceBranch(hook);
        String targetBranch = getTargetBranch(hook);
        if (branchFilter.isBranchAllowed(sourceBranch, targetBranch)) {
            LOGGER.log(Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));
            cancelPendingBuildsIfNecessary(job, hook);
            setCommitStatusPendingIfNecessary(job, hook);
            scheduleBuild(job, createActions(job, hook));
        } else {
            LOGGER.log(Level.INFO, "Source branch {0} or target branch {1} is not allowed", new Object[] {
                sourceBranch, targetBranch
            });
        }
    }

    protected abstract String getTriggerType();

    protected abstract boolean isCiSkip(H hook);

    private void setCommitStatusPendingIfNecessary(Job<?, ?> job, H hook) {
        try {
            String buildName = PendingBuildsHandler.resolvePendingBuildName(job);
            if (StringUtils.isNotBlank(buildName)) {
                GitLabConnectionProperty connectionProperty = job.getProperty(GitLabConnectionProperty.class);
                if (connectionProperty != null) {
                    GitLabClient client = connectionProperty.getClient();
                    if (client != null) {
                        BuildStatusUpdate buildStatusUpdate = retrieveBuildStatusUpdate(hook);
                        try {
                            String ref = StringUtils.removeStart(buildStatusUpdate.getRef(), "refs/tags/");
                            String targetUrl = DisplayURLProvider.get().getJobURL(job);
                            client.changeBuildStatus(
                                    buildStatusUpdate.getProjectId(),
                                    buildStatusUpdate.getSha(),
                                    BuildState.pending,
                                    ref,
                                    buildName,
                                    targetUrl,
                                    BuildState.pending.name());
                        } catch (WebApplicationException | ProcessingException e) {
                            LOGGER.log(Level.SEVERE, "Failed to set build state to pending", e);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "GitLabClient is null");
                    }
                } else {
                    LOGGER.log(Level.WARNING, "GitLabConnectionProperty is null");
                }
            }
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    protected Action[] createActions(Job<?, ?> job, H hook) {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new CauseAction(new GitLabWebHookCause(retrieveCauseData(hook))));
        actions.add(new BranchQueueAction(getSourceBranch(hook)));
        try {
            SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
            GitSCM gitSCM = getGitSCM(item);
            actions.add(createRevisionParameter(hook, gitSCM));
        } catch (NoRevisionToBuildException e) {
            LOGGER.log(
                    Level.WARNING,
                    "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                    new Object[] {hook, (job != null ? job.getFullName() : null)});
        }
        return actions.toArray(new Action[actions.size()]);
    }

    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, H hook) {}

    protected abstract CauseData retrieveCauseData(H hook);

    protected abstract String getSourceBranch(H hook);

    protected abstract String getTargetBranch(H hook);

    protected abstract RevisionParameterAction createRevisionParameter(H hook, GitSCM gitSCM)
            throws NoRevisionToBuildException;

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

    protected void scheduleBuild(Job<?, ?> job, Action[] actions) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob abstractProject) {
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
                if (scm instanceof GitSCM gitSCM) {
                    return gitSCM;
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
