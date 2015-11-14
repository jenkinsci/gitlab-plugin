package com.dabsquared.gitlabjenkins.handlers;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.models.request.GitLabRequest;
import com.dabsquared.gitlabjenkins.types.GitLabBuildStatus;
import com.dabsquared.gitlabjenkins.types.Parameters;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.triggers.SCMTrigger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class GitlabRequestHandler<T extends GitLabRequest> implements Runnable {
    public static final String GITLAB_BRANCH = "gitlabBranch";
    public static final String GITLAB_ACTION_TYPE = "gitlabActionType";
    public static final String NO_COMMIT_HASH = "0000000000000000000000000000000000000000";
    public static final String GITLAB_SOURCE_REPO_NAME = "gitlabSourceRepoName";
    public static final String GITLAB_SOURCE_REPO_URL = "gitlabSourceRepoURL";
    public static final String GITLAB_USER_NAME = "gitlabUserName";
    public static final String GITLAB_USER_EMAIL = "gitlabUserEmail";
    public static final String GITLAB_MERGE_REQUEST_TITLE = "gitlabMergeRequestTitle";
    public static final String GITLAB_MERGE_REQUEST_ID = "gitlabMergeRequestId";
    public static final String GITLAB_MERGE_REQUEST_ASSIGNEE = "gitlabMergeRequestAssignee";
    private static final Logger LOGGER = Logger.getLogger(GitlabRequestHandler.class.getName());
    protected Job job;
    protected T request;
    private boolean addCiMessage;

    public GitlabRequestHandler(Job job, T request, boolean addCiMessage) {
        this.job = job;
        this.request = request;
        this.addCiMessage = addCiMessage;
    }

    public void run() {
        LOGGER.log(Level.INFO, "{0} triggered for push.", job.getName());

        final ParameterizedJobMixIn scheduledJob = new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };

        String name = " #" + job.getNextBuildNumber();
        SCMTrigger.SCMTriggerCause cause = createCause();
        Action[] actions = createActions();

        boolean scheduled;

        if (job instanceof AbstractProject<?, ?>) {
            AbstractProject job_ap = (AbstractProject<?, ?>) job;
            scheduled = job_ap.scheduleBuild(job_ap.getQuietPeriod(), cause, actions);
        } else {
            scheduled = scheduledJob.scheduleBuild(cause);
        }

        if (scheduled) {
            LOGGER.log(Level.INFO, "GitLab Push Request detected in {0}. Triggering {1}", new String[]{job.getName(), name});
        } else {
            LOGGER.log(Level.INFO, "GitLab Push Request detected in {0}. Job is already in the queue.", job.getName());
        }

        if (addCiMessage) {
            request.createCommitStatus(getDesc().getGitlab().instance(), GitLabBuildStatus.PENDING.value(), Jenkins.getInstance().getRootUrl() + job.getUrl());
        }
    }

    protected GitLabPushTrigger.DescriptorImpl getDesc() {
        return GitLabPushTrigger.getDesc();
    }

    protected abstract SCMTrigger.SCMTriggerCause createCause();

    protected abstract Action[] createActions();

    protected abstract String getSourceBranch();

    protected Map<String, ParameterValue> getDefaultParameters(GitLabRequest request) {
        Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
        ParametersDefinitionProperty definitionProperty = (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);

        if (definitionProperty != null) {
            for (ParameterDefinition definition : definitionProperty.getParameterDefinitions()) {
                values.put(definition.getName(), definition.getDefaultParameterValue());
            }
        }

        GitLabPushTrigger.DescriptorImpl desc = getDesc();

        String sourceRepoName = desc.getSourceRepoNameDefault(job);
        String sourceRepoURL = desc.getSourceRepoURLDefault(job).toString();

        if (!desc.getGitlabHostUrl().isEmpty()) {
            // Get source repository if communication to Gitlab is possible
            try {
                sourceRepoName = request.getSourceProject(desc.getGitlab()).getPathWithNamespace();
                sourceRepoURL = request.getSourceProject(desc.getGitlab()).getSshUrl();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Could not fetch source project''s data from Gitlab. '('{0}':' {1}')'", new String[]{ex.toString(), ex.getMessage()});
            }
        }

        values.put(GITLAB_SOURCE_REPO_NAME, new StringParameterValue(GITLAB_SOURCE_REPO_NAME, sourceRepoName));
        values.put(GITLAB_SOURCE_REPO_URL, new StringParameterValue(GITLAB_SOURCE_REPO_URL, sourceRepoURL));
        values.put(Parameters.GITLAB_SOURCE_BRANCH, new StringParameterValue(Parameters.GITLAB_SOURCE_BRANCH, getSourceBranch()));

        return values;
    }

    protected Action[] makeParameterActions(Map<String, ParameterValue> values, Action... extras) {
        List<Action> actions = new ArrayList<Action>(Arrays.asList(extras));
        ParametersAction parametersAction = new ParametersAction(new ArrayList<ParameterValue>(values.values()));
        actions.add(parametersAction);
        return actions.toArray(new Action[actions.size()]);
    }

    protected File getLogFile() {
        return new File(job.getRootDir(), "gitlab-polling.log");
    }
}
