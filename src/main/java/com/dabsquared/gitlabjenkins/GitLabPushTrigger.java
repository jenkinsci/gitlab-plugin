package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.descriptors.GitlabPushTriggerDescriptor;
import com.dabsquared.gitlabjenkins.handlers.MergeRequestHandler;
import com.dabsquared.gitlabjenkins.handlers.PushRequestHandler;
import com.dabsquared.gitlabjenkins.models.cause.GitLabCause;
import com.dabsquared.gitlabjenkins.models.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.models.cause.GitLabPushCause;
import com.dabsquared.gitlabjenkins.models.request.GitLabMergeRequest;
import com.dabsquared.gitlabjenkins.models.request.GitLabPushRequest;
import com.dabsquared.gitlabjenkins.types.Emoji;
import com.dabsquared.gitlabjenkins.types.GitLabBuildStatus;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import org.apache.commons.lang.text.StrSubstitutor;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<Job<?, ?>> {
    private static final Logger LOG = Logger.getLogger(GitLabPushTrigger.class.getName());
    private static final String BUILD_RESULT_KEY = "buildResult";
    private static final String BUILD_URL_KEY = "buildUrl";
    private static final String ICON_KEY = "icon";

    private final String triggerOpenMergeRequestOnPush;
    private final String includeBranchesSpec;
    private final String excludeBranchesSpec;
    private final String buildAbortedMsg;
    private final String buildSuccessMsg;
    private final String buildFailureMsg;
    private final String mergeRequestAcceptMsg;
    private final String buildUnstableMsg;

    private boolean customMessages = false;
    private boolean triggerOnPush = true;
    private boolean triggerOnMergeRequest = true;
    private boolean ciSkip = true;
    private boolean setBuildDescription = true;
    private boolean addNoteOnMergeRequest = true;
    private boolean addCiMessage = false;
    private boolean addVoteOnMergeRequest = true;
    private boolean allowAllBranches = false;
    private boolean acceptMergeRequestOnSuccess = false;
    private Map<String, String> messageByResult;

    @DataBoundConstructor
    public GitLabPushTrigger(boolean triggerOnPush, boolean triggerOnMergeRequest, String triggerOpenMergeRequestOnPush,
                             boolean ciSkip, boolean setBuildDescription, boolean addNoteOnMergeRequest, boolean addCiMessage, boolean addVoteOnMergeRequest,
                             boolean acceptMergeRequestOnSuccess, boolean allowAllBranches, String includeBranchesSpec, String excludeBranchesSpec,
                             String buildAbortedMsg, String buildSuccessMsg, String buildFailureMsg, String mergeRequestAcceptMsg, String buildUnstableMsg, boolean customMessages) {
        this.customMessages = customMessages;
        if (customMessages) {
            this.buildAbortedMsg = buildAbortedMsg;
            this.buildSuccessMsg = buildSuccessMsg;
            this.buildFailureMsg = buildFailureMsg;
            this.buildUnstableMsg = buildUnstableMsg;
            this.mergeRequestAcceptMsg = mergeRequestAcceptMsg;
        } else {
            this.buildAbortedMsg = this.buildFailureMsg = this.buildSuccessMsg = this.buildUnstableMsg = GitlabPushTriggerDescriptor.DEFAULT_BUILD_MSG;
            this.mergeRequestAcceptMsg = GitlabPushTriggerDescriptor.DEFAULT_MERGE_REQUEST_ACCEPT_MSG;
        }
        this.triggerOnPush = triggerOnPush;
        this.triggerOnMergeRequest = triggerOnMergeRequest;
        this.triggerOpenMergeRequestOnPush = triggerOpenMergeRequestOnPush;
        this.ciSkip = ciSkip;
        this.setBuildDescription = setBuildDescription;
        this.addNoteOnMergeRequest = addNoteOnMergeRequest;
        this.addCiMessage = addCiMessage;
        this.addVoteOnMergeRequest = addVoteOnMergeRequest;
        this.allowAllBranches = allowAllBranches;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.acceptMergeRequestOnSuccess = acceptMergeRequestOnSuccess;

        this.messageByResult = new HashMap<String, String>();
        this.messageByResult.put(Result.SUCCESS.toString(), this.buildSuccessMsg);
        this.messageByResult.put(Result.FAILURE.toString(), this.buildFailureMsg);
        this.messageByResult.put(Result.ABORTED.toString(), this.buildAbortedMsg);
        this.messageByResult.put(Result.UNSTABLE.toString(), this.buildUnstableMsg);
    }

    public boolean getTriggerOnPush() {
        return triggerOnPush;
    }

    public boolean getTriggerOnMergeRequest() {
        return triggerOnMergeRequest;
    }

    public String getTriggerOpenMergeRequestOnPush() {
        return triggerOpenMergeRequestOnPush;
    }

    public boolean getSetBuildDescription() {
        return setBuildDescription;
    }

    public boolean getAddNoteOnMergeRequest() {
        return addNoteOnMergeRequest;
    }

    public boolean getAddVoteOnMergeRequest() {
        return addVoteOnMergeRequest;
    }

    public boolean getAcceptMergeRequestOnSuccess() {
        return acceptMergeRequestOnSuccess;
    }

    public boolean getAllowAllBranches() {
        return allowAllBranches;
    }

    public boolean getAddCiMessage() {
        return addCiMessage;
    }

    public boolean getCiSkip() {
        return ciSkip;
    }

    public String getBuildSuccessMsg() {
        return buildSuccessMsg;
    }

    public String getBuildFailureMsg() {
        return buildFailureMsg;
    }

    public String getMergeRequestAcceptMsg() {
        return mergeRequestAcceptMsg;
    }

    public String getBuildAbortedMsg() {
        return buildAbortedMsg;
    }

    public String getBuildUnstableMsg() {
        return buildUnstableMsg;
    }

    public boolean isCustomMessages() {
        return customMessages;
    }

    private boolean isBranchAllowed(final String branchName) {
        final List<String> exclude = DescriptorImpl.splitBranchSpec(this.getExcludeBranchesSpec());
        final List<String> include = DescriptorImpl.splitBranchSpec(this.getIncludeBranchesSpec());
        if (exclude.isEmpty() && include.isEmpty()) {
            return true;
        }

        final AntPathMatcher matcher = new AntPathMatcher();
        for (final String pattern : exclude) {
            if (matcher.match(pattern, branchName)) {
                return false;
            }
        }
        for (final String pattern : include) {
            if (matcher.match(pattern, branchName)) {
                return true;
            }
        }

        return false;
    }

    public String getIncludeBranchesSpec() {
        return this.includeBranchesSpec == null ? "" : this.includeBranchesSpec;
    }

    public String getExcludeBranchesSpec() {
        return this.excludeBranchesSpec == null ? "" : this.excludeBranchesSpec;
    }

    // executes when the Trigger receives a push request
    public void onPost(final GitLabPushRequest req) {
        if (!triggerOnPush) {
            return;
        }

        PushRequestHandler handler = new PushRequestHandler(job, req, addCiMessage);
        if (allowAllBranches || this.isBranchAllowed(handler.getSourceBranch())) {
            getDescriptor().getQueue().execute(handler);
        }
    }

    // executes when the Trigger receives a merge request
    public void onPost(final GitLabMergeRequest req) {
        if (!triggerOnMergeRequest) {
            return;
        }
        getDescriptor().getQueue().execute(new MergeRequestHandler(job, req, addCiMessage));
    }

    private void setBuildCauseInJob(Run run) {
        if (setBuildDescription) {
            Cause cause = run.getCause(GitLabCause.class);
            String desc = null;
            if (cause != null) {
                desc = cause.getShortDescription();
            }
            if (desc != null && desc.length() > 0) {
                try {
                    run.setDescription(desc);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to set build cause", e);
                }
            }
        }
    }

    public void onCompleted(Run run) {
        Cause mergeCause = run.getCause(GitLabMergeCause.class);
        if (mergeCause != null && mergeCause instanceof GitLabMergeCause) {
            onCompletedRequest(run, (GitLabMergeCause) mergeCause);
        }

        Cause pushCause = run.getCause(GitLabPushCause.class);
        if (pushCause != null && pushCause instanceof GitLabPushCause) {
            onCompletedRequest(run, (GitLabPushCause) pushCause);
        }

    }

    private void onCompletedRequest(Run run, GitLabPushCause cause) {
        if (addCiMessage) {
            String status = GitLabBuildStatus.valueOf(run.getResult()).value();
            cause.getRequest().createCommitStatus(this.getDescriptor().getGitlab().instance(), status, Jenkins.getInstance().getRootUrl() + run.getUrl());
        }
    }

    private void onCompletedRequest(Run run, GitLabMergeCause cause) {
        if (acceptMergeRequestOnSuccess && run.getResult() == Result.SUCCESS) {
            try {
                GitlabProject project = new GitlabProject();
                project.setId(cause.getRequest().getObjectAttribute().getTargetProjectId());
                this.getDescriptor().getGitlab().instance().acceptMergeRequest(
                        project,
                        cause.getRequest().getObjectAttribute().getId(),
                        mergeRequestAcceptMsg);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to accept merge request", e);
            }
        }
        if (addNoteOnMergeRequest) {
            String msg = getCompleteMessage(run);
            try {
                GitlabProject proj = new GitlabProject();
                proj.setId(cause.getRequest().getObjectAttribute().getTargetProjectId());
                org.gitlab.api.models.GitlabMergeRequest mergeRequest = this.getDescriptor().getGitlab().instance().getMergeRequest(proj, cause.getRequest().getObjectAttribute().getId());
                this.getDescriptor().getGitlab().instance().createNote(mergeRequest, msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (addCiMessage) {
            String commitStatus = GitLabBuildStatus.valueOf(run.getResult()).value();
            cause.getRequest().createCommitStatus(this.getDescriptor().getGitlab().instance(), commitStatus, Jenkins.getInstance().getRootUrl() + run.getUrl());
        }
    }

    private String getCompleteMessage(Run run) {
        String icon = "";
        StringBuilder msg = new StringBuilder();
        Result result = run.getResult();

        if (result == Result.SUCCESS) {
            icon = addVoteOnMergeRequest ? Emoji.PLUS_ONE : Emoji.WHITE_CHECK_MARK;
        } else if (result == Result.FAILURE || result == Result.UNSTABLE) {
            icon = addVoteOnMergeRequest ? Emoji.MINUS_ONE : Emoji.ANGUISHED;
        }

        String buildUrl = Jenkins.getInstance().getRootUrl() + run.getUrl();
        Map<String, String> messageParams = new HashMap<String, String>();

        messageParams.put(ICON_KEY, icon);
        messageParams.put(BUILD_RESULT_KEY, run.getResult().color.getDescription());
        messageParams.put(BUILD_URL_KEY, buildUrl);

        String msgTemplate = messageByResult.get(result.toString());
        if (msgTemplate == null) {
            msgTemplate = "";
        }

        msg.append(StrSubstitutor.replace(msgTemplate, messageParams));
        return msg.toString();
    }

    public void onStarted(Run run) {
        setBuildCauseInJob(run);

        Cause cause = run.getCause(GitLabMergeCause.class);
        if (cause != null && cause instanceof GitLabCause && addCiMessage) {
            String targetUrl = Jenkins.getInstance().getRootUrl() + run.getUrl();
            GitlabAPI instance = this.getDescriptor().getGitlab().instance();
            ((GitLabCause) cause).getRequest().createCommitStatus(instance, GitLabBuildStatus.RUNNING.value(), targetUrl);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.getInstance();
    }

    @Extension
    public static DescriptorImpl getDesc() {
        return DescriptorImpl.getInstance();
    }

    public static class DescriptorImpl extends GitlabPushTriggerDescriptor {
        public static class DescriptorImplHolder {
            public static final DescriptorImpl INSTANCE = new DescriptorImpl();
        }

        public static DescriptorImpl getInstance() {
            return DescriptorImplHolder.INSTANCE;
        }
    }
}
