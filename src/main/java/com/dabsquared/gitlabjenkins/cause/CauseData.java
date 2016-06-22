package com.dabsquared.gitlabjenkins.cause;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Robin Müller
 */
public class CauseData {
    private final ActionType actionType;
    private final Integer projectId;
    private final String branch;
    private final String sourceBranch;
    private final String userName;
    private final String userEmail;
    private final String sourceRepoHomepage;
    private final String sourceRepoName;
    private final String sourceNamespace;
    private final String sourceRepoUrl;
    private final String sourceRepoSshUrl;
    private final String sourceRepoHttpUrl;
    private final String mergeRequestTitle;
    private final String mergeRequestDescription;
    private final Integer mergeRequestId;
    private final Integer mergeRequestIid;
    private final String targetBranch;
    private final String targetRepoName;
    private final String targetNamespace;
    private final String targetRepoSshUrl;
    private final String targetRepoHttpUrl;
    private final String triggeredByUser;

    @GeneratePojoBuilder(withFactoryMethod = "*")
    CauseData(ActionType actionType, Integer projectId, String branch, String sourceBranch, String userName, String userEmail,
              String sourceRepoHomepage, String sourceRepoName, String sourceNamespace, String sourceRepoUrl, String sourceRepoSshUrl,
              String sourceRepoHttpUrl, String mergeRequestTitle, String mergeRequestDescription, Integer mergeRequestId, Integer mergeRequestIid,
              String targetBranch, String targetRepoName, String targetNamespace, String targetRepoSshUrl, String targetRepoHttpUrl,
              String triggeredByUser) {
        this.actionType = checkNotNull(actionType, "actionType must not be null.");
        this.projectId = checkNotNull(projectId, "projectId must not be null.");
        this.branch = checkNotNull(branch, "branch must not be null.");
        this.sourceBranch = checkNotNull(sourceBranch, "sourceBranch must not be null.");
        this.userName = checkNotNull(userName, "userName must not be null.");
        this.userEmail = userEmail == null ? "" : userEmail;
        this.sourceRepoHomepage = sourceRepoHomepage == null ? "" : sourceRepoHomepage;
        this.sourceRepoName = checkNotNull(sourceRepoName, "sourceRepoName must not be null.");
        this.sourceNamespace = checkNotNull(sourceNamespace, "sourceNamespace must not be null.");
        this.sourceRepoUrl = sourceRepoUrl == null ? sourceRepoSshUrl : sourceRepoUrl;
        this.sourceRepoSshUrl = checkNotNull(sourceRepoSshUrl, "sourceRepoSshUrl must not be null.");
        this.sourceRepoHttpUrl = checkNotNull(sourceRepoHttpUrl, "sourceRepoHttpUrl must not be null.");
        this.mergeRequestTitle = checkNotNull(mergeRequestTitle, "mergeRequestTitle must not be null.");
        this.mergeRequestDescription = mergeRequestDescription == null ? "" : mergeRequestDescription;
        this.mergeRequestId = mergeRequestId;
        this.mergeRequestIid = mergeRequestIid;
        this.targetBranch = checkNotNull(targetBranch, "targetBranch must not be null.");
        this.targetRepoName = checkNotNull(targetRepoName, "targetRepoName must not be null.");
        this.targetNamespace = checkNotNull(targetNamespace, "targetNamespace must not be null.");
        this.targetRepoSshUrl = checkNotNull(targetRepoSshUrl, "targetRepoSshUrl must not be null.");
        this.targetRepoHttpUrl = checkNotNull(targetRepoHttpUrl, "targetRepoHttpUrl must not be null.");
        this.triggeredByUser = checkNotNull(triggeredByUser, "triggeredByUser must not be null.");
    }

    public Map<String, String> getBuildVariables() {
        HashMap<String, String> variables = new HashMap<>();
        variables.put("gitlabBranch", branch);
        variables.put("gitlabSourceBranch", sourceBranch);
        variables.put("gitlabActionType", actionType.name());
        variables.put("gitlabUserName", userName);
        variables.put("gitlabUserEmail", userEmail);
        variables.put("gitlabSourceRepoHomepage", sourceRepoHomepage);
        variables.put("gitlabSourceRepoName", sourceRepoName);
        variables.put("gitlabSourceNamespace", sourceNamespace);
        variables.put("gitlabSourceRepoURL", sourceRepoUrl);
        variables.put("gitlabSourceRepoSshUrl", sourceRepoSshUrl);
        variables.put("gitlabSourceRepoHttpUrl", sourceRepoHttpUrl);
        variables.put("gitlabMergeRequestTitle", mergeRequestTitle);
        variables.put("gitlabMergeRequestDescription", mergeRequestDescription);
        variables.put("gitlabMergeRequestId", mergeRequestId == null ? "" : mergeRequestId.toString());
        variables.put("gitlabMergeRequestIid", mergeRequestIid == null ? "" : mergeRequestIid.toString());
        variables.put("gitlabTargetBranch", targetBranch);
        variables.put("gitlabTargetRepoName", targetRepoName);
        variables.put("gitlabTargetNamespace", targetNamespace);
        variables.put("gitlabTargetRepoSshUrl", targetRepoSshUrl);
        variables.put("gitlabTargetRepoHttpUrl", targetRepoHttpUrl);
        return variables;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public String getBranch() {
        return branch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSourceRepoHomepage() {
        return sourceRepoHomepage;
    }

    public String getSourceRepoName() {
        return sourceRepoName;
    }

    public String getSourceNamespace() {
        return sourceNamespace;
    }

    public String getSourceRepoUrl() {
        return sourceRepoUrl;
    }

    public String getSourceRepoSshUrl() {
        return sourceRepoSshUrl;
    }

    public String getSourceRepoHttpUrl() {
        return sourceRepoHttpUrl;
    }

    public String getMergeRequestTitle() {
        return mergeRequestTitle;
    }

    public String getMergeRequestDescription() {
        return mergeRequestDescription;
    }

    public Integer getMergeRequestId() {
        return mergeRequestId;
    }

    public Integer getMergeRequestIid() {
        return mergeRequestIid;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getTargetRepoName() {
        return targetRepoName;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getTargetRepoSshUrl() {
        return targetRepoSshUrl;
    }

    public String getTargetRepoHttpUrl() {
        return targetRepoHttpUrl;
    }

    public String getTriggeredByUser() {
        return triggeredByUser;
    }

    String getShortDescription() {
        return actionType.getShortDescription(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CauseData causeData = (CauseData) o;
        return new EqualsBuilder()
                .append(projectId, causeData.projectId)
                .append(branch, causeData.branch)
                .append(sourceBranch, causeData.sourceBranch)
                .append(actionType, causeData.actionType)
                .append(userName, causeData.userName)
                .append(userEmail, causeData.userEmail)
                .append(sourceRepoHomepage, causeData.sourceRepoHomepage)
                .append(sourceRepoName, causeData.sourceRepoName)
                .append(sourceRepoUrl, causeData.sourceRepoUrl)
                .append(sourceRepoSshUrl, causeData.sourceRepoSshUrl)
                .append(sourceRepoHttpUrl, causeData.sourceRepoHttpUrl)
                .append(mergeRequestTitle, causeData.mergeRequestTitle)
                .append(mergeRequestDescription, causeData.mergeRequestDescription)
                .append(mergeRequestId, causeData.mergeRequestId)
                .append(mergeRequestIid, causeData.mergeRequestIid)
                .append(targetBranch, causeData.targetBranch)
                .append(targetRepoName, causeData.targetRepoName)
                .append(targetRepoSshUrl, causeData.targetRepoSshUrl)
                .append(targetRepoHttpUrl, causeData.targetRepoHttpUrl)
                .append(triggeredByUser, causeData.triggeredByUser)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(projectId)
                .append(branch)
                .append(sourceBranch)
                .append(actionType)
                .append(userName)
                .append(userEmail)
                .append(sourceRepoHomepage)
                .append(sourceRepoName)
                .append(sourceRepoUrl)
                .append(sourceRepoSshUrl)
                .append(sourceRepoHttpUrl)
                .append(mergeRequestTitle)
                .append(mergeRequestDescription)
                .append(mergeRequestId)
                .append(mergeRequestIid)
                .append(targetBranch)
                .append(targetRepoName)
                .append(targetRepoSshUrl)
                .append(targetRepoHttpUrl)
                .append(triggeredByUser)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("projectId", projectId)
                .append("branch", branch)
                .append("sourceBranch", sourceBranch)
                .append("actionType", actionType)
                .append("userName", userName)
                .append("userEmail", userEmail)
                .append("sourceRepoHomepage", sourceRepoHomepage)
                .append("sourceRepoName", sourceRepoName)
                .append("sourceRepoUrl", sourceRepoUrl)
                .append("sourceRepoSshUrl", sourceRepoSshUrl)
                .append("sourceRepoHttpUrl", sourceRepoHttpUrl)
                .append("mergeRequestTitle", mergeRequestTitle)
                .append("mergeRequestDescription", mergeRequestDescription)
                .append("mergeRequestId", mergeRequestId)
                .append("mergeRequestIid", mergeRequestIid)
                .append("targetBranch", targetBranch)
                .append("targetRepoName", targetRepoName)
                .append("targetRepoSshUrl", targetRepoSshUrl)
                .append("targetRepoHttpUrl", targetRepoHttpUrl)
                .append("triggeredByUser", triggeredByUser)
                .toString();
    }

    public enum ActionType {
        PUSH {
            @Override
            String getShortDescription(CauseData data) {
                String pushedBy = data.getTriggeredByUser();
                if (pushedBy == null) {
                    return "Started by GitLab push";
                } else {
                    return String.format("Started by GitLab push by %s", pushedBy);
                }
            }
        }, MERGE {
            @Override
            String getShortDescription(CauseData data) {
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch()) ? "" : data.getSourceNamespace() + "/";
                return "GitLab Merge Request #" + data.getMergeRequestIid() + ": " + forkNamespace + data.getSourceBranch() + " => " + data.getTargetBranch();
            }
        }, NOTE {
            @Override
            String getShortDescription(CauseData data) {
                String triggeredBy = data.getTriggeredByUser();
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch()) ? "" : data.getSourceNamespace() + "/";
                return "Triggered by " + triggeredBy + " GitLab Merge Request #" + data.getMergeRequestIid() + ": " + forkNamespace + data.getSourceBranch() + " => " + data.getTargetBranch();
            }
        };

        abstract String getShortDescription(CauseData data);
    }
}
