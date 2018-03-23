package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import hudson.AbortException;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.TopLevelItem;
import hudson.slaves.WorkspaceList;
import hudson.triggers.Trigger;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jenkins.model.Jenkins.getInstance;

public final class TriggerUtil {
    private static final Logger LOGGER = Logger.getLogger(TriggerUtil.class.getName());
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("(triggers\\s{0,}\\{\\s{0,}gitlab\\s{0,}\\()" +
        "([a-zA-Z: ,\"*]+)");

    private TriggerUtil() {
    }

    public static <T extends WebHook> GitLabPushTrigger getFromJob(Item project, T webHook) {
        if (project instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob job = (ParameterizedJobMixIn.ParameterizedJob) project;
            for (Trigger trigger : job.getTriggers().values()) {
                if (trigger instanceof GitLabPushTrigger) {
                    return (GitLabPushTrigger) trigger;
                }
            }
        }

        if (project instanceof WorkflowMultiBranchProject) {
            if (webHook == null) {
                return null;
            }

            WorkflowMultiBranchProject multiBranchProject = (WorkflowMultiBranchProject) project;

            FilePath dir;
            Node node = getInstance();
            Objects.requireNonNull(node, "Jenkins node not active");
            if (multiBranchProject.getParent() instanceof TopLevelItem) {
                FilePath baseWorkspace = node.getWorkspaceFor((TopLevelItem) multiBranchProject.getParent());
                Objects.requireNonNull(baseWorkspace, "Could not get project workspace");
                dir = getFilePathWithSuffix(baseWorkspace);
            } else { // should not happen, but just in case:
                dir = new FilePath(multiBranchProject.getRootDir());
            }

            try {
                String branch = webHook.getEventSourceBranch();
                // Uses same constant as in org.jenkinsci.plugins.workflow.multibranch.WorkflowBranchProjectFactory
                FilePath scriptFile = dir.child("branches").child(branch).child("workspace").child("Jenkinsfile");
                if (!scriptFile.absolutize()
                    .getRemote()
                    .replace('\\', '/')
                    .startsWith(dir.absolutize().getRemote().replace('\\', '/') + '/')) { // TODO JENKINS-26838
                    throw new IOException(scriptFile + " is not inside " + dir);
                }
                if (!scriptFile.exists()) {
                    throw new AbortException(scriptFile + " not found");
                }
                String script = scriptFile.readToString().replace("\n", "");
                GitLabPushTrigger trigger = parseTriggerFromPipeline(script);
                trigger.setJob(((WorkflowMultiBranchProject) project).getItemByBranchName(branch));
                return trigger;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static GitLabPushTrigger parseTriggerFromPipeline(String script) throws AbortException {
        Matcher matcher = TRIGGER_PATTERN.matcher(script);
        if (matcher.find()) {
            String group = matcher.group(2).trim();
            LOGGER.info("Pattern match : " + group);
            String[] parts = group.split(",");
            Map<String, String> triggerConfig = new HashMap<>();
            for (String part : parts) {
                String[] keyValue = part.split(":");
                String key = keyValue[0].trim();
                String value = keyValue[1].replace("\"", "").trim();
                triggerConfig.put(key, value);
            }

            GitLabPushTrigger trigger = new GitLabPushTrigger();
            String triggerOnPush = triggerConfig.get("triggerOnPush");
            if (triggerOnPush != null) {
                trigger.setTriggerOnPush(Boolean.valueOf(triggerOnPush.trim()));
            }
            String triggerOnMergeRequest = triggerConfig.get("triggerOnMergeRequest");
            if (triggerOnMergeRequest != null) {
                trigger.setTriggerOnMergeRequest(Boolean.valueOf(triggerOnMergeRequest.trim()));
            }
            String triggerOnNoteRequest = triggerConfig.get("triggerOnNoteRequest");
            if (triggerOnNoteRequest != null) {
                trigger.setTriggerOnNoteRequest(Boolean.valueOf(triggerOnNoteRequest.trim()));
            }
            String skipWorkInProgressMergeRequest = triggerConfig.get("skipWorkInProgressMergeRequest");
            if (skipWorkInProgressMergeRequest != null) {
                trigger.setSkipWorkInProgressMergeRequest(Boolean.valueOf(skipWorkInProgressMergeRequest.trim()));
            }
            String ciSkip = triggerConfig.get("ciSkip");
            if (ciSkip != null) {
                trigger.setCiSkip(Boolean.valueOf(ciSkip.trim()));
            }
            String setBuildDescription = triggerConfig.get("setBuildDescription");
            if (setBuildDescription != null) {
                trigger.setSetBuildDescription(Boolean.valueOf(setBuildDescription.trim()));
            }
            String addNoteOnMergeRequest = triggerConfig.get("addNoteOnMergeRequest");
            if (addNoteOnMergeRequest != null) {
                trigger.setAddNoteOnMergeRequest(Boolean.valueOf(addNoteOnMergeRequest.trim()));
            }
            String addCiMessage = triggerConfig.get("addCiMessage");
            if (addCiMessage != null) {
                trigger.setAddCiMessage(Boolean.valueOf(addCiMessage.trim()));
            }
            String addVoteOnMergeRequest = triggerConfig.get("addVoteOnMergeRequest");
            if (addVoteOnMergeRequest != null) {
                trigger.setAddVoteOnMergeRequest(Boolean.valueOf(addVoteOnMergeRequest.trim()));
            }
            String acceptMergeRequestOnSuccess = triggerConfig.get("acceptMergeRequestOnSuccess");
            if (acceptMergeRequestOnSuccess != null) {
                trigger.setAcceptMergeRequestOnSuccess(Boolean.valueOf(acceptMergeRequestOnSuccess.trim()));
            }
            String triggerOpenMergeRequestOnPush = triggerConfig.get("triggerOpenMergeRequestOnPush");
            if (triggerOpenMergeRequestOnPush != null && !triggerOpenMergeRequestOnPush.isEmpty()) {
                trigger.setTriggerOpenMergeRequestOnPush(TriggerOpenMergeRequest.valueOf
                    (triggerOpenMergeRequestOnPush.toLowerCase()));
            }
            String noteRegex = triggerConfig.get("noteRegex");
            if (noteRegex != null && !noteRegex.isEmpty()) {
                trigger.setNoteRegex(noteRegex);
            }
            String branchFilterType = triggerConfig.get("branchFilterType");
            if (branchFilterType != null && !branchFilterType.isEmpty()) {
                trigger.setBranchFilterType(BranchFilterType.valueOf(branchFilterType));
            }
            String includeBranchesSpec = triggerConfig.get("includeBranchesSpec");
            if (includeBranchesSpec != null && !includeBranchesSpec.isEmpty()) {
                trigger.setIncludeBranchesSpec(includeBranchesSpec);
            }
            String excludeBranchesSpec = triggerConfig.get("excludeBranchesSpec");
            if (excludeBranchesSpec != null && !excludeBranchesSpec.isEmpty()) {
                trigger.setExcludeBranchesSpec(excludeBranchesSpec);
            }
            String secretToken = triggerConfig.get("secretToken");
            if (secretToken != null && !secretToken.isEmpty()) {
                trigger.setSecretToken(secretToken);
            }
            return trigger;
        } else {
            LOGGER.warning("No gitlab trigger in script");
            throw new AbortException("No gitlab trigger in script");
        }
    }

    private static FilePath getFilePathWithSuffix(FilePath baseWorkspace) {
        return baseWorkspace.withSuffix(getFilePathSuffix() + "script");
    }

    private static String getFilePathSuffix() {
        return System.getProperty(WorkspaceList.class.getName(), "@");
    }
}
