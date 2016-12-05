package com.dabsquared.gitlabjenkins.cause;

import hudson.markup.EscapedMarkupFormatter;
import jenkins.model.Jenkins;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Robin Müller
 */
public final class CauseData {
    private final ActionType actionType;
    private final Integer sourceProjectId;
    private final Integer targetProjectId;
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
    private final String before;
    private final String after;
    private final String lastCommit;
    private final String targetProjectUrl;
    private final String triggerPhrase;

    @GeneratePojoBuilder(withFactoryMethod = "*")
    CauseData(ActionType actionType, Integer sourceProjectId, Integer targetProjectId, String branch, String sourceBranch, String userName,
              String userEmail, String sourceRepoHomepage, String sourceRepoName, String sourceNamespace, String sourceRepoUrl,
              String sourceRepoSshUrl, String sourceRepoHttpUrl, String mergeRequestTitle, String mergeRequestDescription, Integer mergeRequestId,
              Integer mergeRequestIid, String targetBranch, String targetRepoName, String targetNamespace, String targetRepoSshUrl,
              String targetRepoHttpUrl, String triggeredByUser, String before, String after, String lastCommit, String targetProjectUrl,
              String triggerPhrase) {
        this.actionType = checkNotNull(actionType, "actionType must not be null.");
        this.sourceProjectId = checkNotNull(sourceProjectId, "sourceProjectId must not be null.");
        this.targetProjectId = checkNotNull(targetProjectId, "targetProjectId must not be null.");
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
        this.before = before == null ? "" : before;
        this.after = after == null ? "" : after;
        this.lastCommit = checkNotNull(lastCommit, "lastCommit must not be null");
        this.targetProjectUrl = targetProjectUrl;
        this.triggerPhrase = triggerPhrase;
    }

    public Map<String, String> getBuildVariables() {
        MapWrapper<String, String> variables = new MapWrapper<>(new HashMap<String, String>());
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
        variables.put("gitlabMergeRequestLastCommit", lastCommit);
        variables.put("gitlabTargetBranch", targetBranch);
        variables.put("gitlabTargetRepoName", targetRepoName);
        variables.put("gitlabTargetNamespace", targetNamespace);
        variables.put("gitlabTargetRepoSshUrl", targetRepoSshUrl);
        variables.put("gitlabTargetRepoHttpUrl", targetRepoHttpUrl);
        variables.put("gitlabBefore", before);
        variables.put("gitlabAfter", after);
        variables.pufIfNotNull("gitlabTriggerPhrase", triggerPhrase);
        return variables;
    }

    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public Integer getTargetProjectId() {
        return targetProjectId;
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

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public String getTargetProjectUrl() {
        return targetProjectUrl;
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
            .append(actionType, causeData.actionType)
            .append(sourceProjectId, causeData.sourceProjectId)
            .append(targetProjectId, causeData.targetProjectId)
            .append(branch, causeData.branch)
            .append(sourceBranch, causeData.sourceBranch)
            .append(userName, causeData.userName)
            .append(userEmail, causeData.userEmail)
            .append(sourceRepoHomepage, causeData.sourceRepoHomepage)
            .append(sourceRepoName, causeData.sourceRepoName)
            .append(sourceNamespace, causeData.sourceNamespace)
            .append(sourceRepoUrl, causeData.sourceRepoUrl)
            .append(sourceRepoSshUrl, causeData.sourceRepoSshUrl)
            .append(sourceRepoHttpUrl, causeData.sourceRepoHttpUrl)
            .append(mergeRequestTitle, causeData.mergeRequestTitle)
            .append(mergeRequestDescription, causeData.mergeRequestDescription)
            .append(mergeRequestId, causeData.mergeRequestId)
            .append(mergeRequestIid, causeData.mergeRequestIid)
            .append(targetBranch, causeData.targetBranch)
            .append(targetRepoName, causeData.targetRepoName)
            .append(targetNamespace, causeData.targetNamespace)
            .append(targetRepoSshUrl, causeData.targetRepoSshUrl)
            .append(targetRepoHttpUrl, causeData.targetRepoHttpUrl)
            .append(triggeredByUser, causeData.triggeredByUser)
            .append(before, causeData.before)
            .append(after, causeData.after)
            .append(lastCommit, causeData.lastCommit)
            .append(targetProjectUrl, causeData.targetProjectUrl)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(actionType)
            .append(sourceProjectId)
            .append(targetProjectId)
            .append(branch)
            .append(sourceBranch)
            .append(userName)
            .append(userEmail)
            .append(sourceRepoHomepage)
            .append(sourceRepoName)
            .append(sourceNamespace)
            .append(sourceRepoUrl)
            .append(sourceRepoSshUrl)
            .append(sourceRepoHttpUrl)
            .append(mergeRequestTitle)
            .append(mergeRequestDescription)
            .append(mergeRequestId)
            .append(mergeRequestIid)
            .append(targetBranch)
            .append(targetRepoName)
            .append(targetNamespace)
            .append(targetRepoSshUrl)
            .append(targetRepoHttpUrl)
            .append(triggeredByUser)
            .append(before)
            .append(after)
            .append(lastCommit)
            .append(targetProjectUrl)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("actionType", actionType)
            .append("sourceProjectId", sourceProjectId)
            .append("targetProjectId", targetProjectId)
            .append("branch", branch)
            .append("sourceBranch", sourceBranch)
            .append("userName", userName)
            .append("userEmail", userEmail)
            .append("sourceRepoHomepage", sourceRepoHomepage)
            .append("sourceRepoName", sourceRepoName)
            .append("sourceNamespace", sourceNamespace)
            .append("sourceRepoUrl", sourceRepoUrl)
            .append("sourceRepoSshUrl", sourceRepoSshUrl)
            .append("sourceRepoHttpUrl", sourceRepoHttpUrl)
            .append("mergeRequestTitle", mergeRequestTitle)
            .append("mergeRequestDescription", mergeRequestDescription)
            .append("mergeRequestId", mergeRequestId)
            .append("mergeRequestIid", mergeRequestIid)
            .append("targetBranch", targetBranch)
            .append("targetRepoName", targetRepoName)
            .append("targetNamespace", targetNamespace)
            .append("targetRepoSshUrl", targetRepoSshUrl)
            .append("targetRepoHttpUrl", targetRepoHttpUrl)
            .append("triggeredByUser", triggeredByUser)
            .append("before", before)
            .append("after", after)
            .append("lastCommit", lastCommit)
            .append("targetProjectUrl", targetProjectUrl)
            .toString();
    }

    public enum ActionType {
        PUSH {
            @Override
            String getShortDescription(CauseData data) {
                String pushedBy = data.getTriggeredByUser();
                if (pushedBy == null) {
                    return Messages.GitLabWebHookCause_ShortDescription_PushHook_noUser();
                } else {
                    return Messages.GitLabWebHookCause_ShortDescription_PushHook(pushedBy);
                }
            }
        }, MERGE {
            @Override
            String getShortDescription(CauseData data) {
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch()) ? "" : data.getSourceNamespace() + "/";
                if (Jenkins.getActiveInstance().getMarkupFormatter() instanceof EscapedMarkupFormatter || data.getTargetProjectUrl() == null) {
                    return Messages.GitLabWebHookCause_ShortDescription_MergeRequestHook_plain(String.valueOf(data.getMergeRequestIid()),
                                                                                               forkNamespace + data.getSourceBranch(),
                                                                                               data.getTargetBranch());
                } else {
                    return Messages.GitLabWebHookCause_ShortDescription_MergeRequestHook_html(String.valueOf(data.getMergeRequestIid()),
                                                                                              forkNamespace + data.getSourceBranch(),
                                                                                              data.getTargetBranch(),
                                                                                              data.getTargetProjectUrl());
                }
            }
        }, NOTE {
            @Override
            String getShortDescription(CauseData data) {
                String triggeredBy = data.getTriggeredByUser();
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch()) ? "" : data.getSourceNamespace() + "/";
                if (Jenkins.getActiveInstance().getMarkupFormatter() instanceof EscapedMarkupFormatter || data.getTargetProjectUrl() == null) {
                    return Messages.GitLabWebHookCause_ShortDescription_NoteHook_plain(triggeredBy,
                                                                                       String.valueOf(data.getMergeRequestIid()),
                                                                                       forkNamespace + data.getSourceBranch(),
                                                                                       data.getTargetBranch());
                } else {
                    return Messages.GitLabWebHookCause_ShortDescription_NoteHook_html(triggeredBy,
                                                                                      String.valueOf(data.getMergeRequestIid()),
                                                                                      forkNamespace + data.getSourceBranch(),
                                                                                      data.getTargetBranch(),
                                                                                      data.getTargetProjectUrl());
                }
            }
        };

        abstract String getShortDescription(CauseData data);
    }

    private static class MapWrapper<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> map;

        public MapWrapper(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public V put(K key, V value) {
            return map.put(key, value);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return map.entrySet();
        }

        public void pufIfNotNull(K key, V value) {
            if (value != null) {
                map.put(key, value);
            }
        }
    }
}
