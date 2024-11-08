package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import hudson.markup.EscapedMarkupFormatter;
import java.util.*;
import jenkins.model.Jenkins;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author Robin Müller
 */
@ExportedBean
public final class CauseData {
    private final ActionType actionType;
    private final Integer sourceProjectId;
    private final Integer targetProjectId;
    private final Integer projectId;
    private final String branch;
    private final String sourceBranch;
    private final String userName;
    private final String userUsername;
    private final String userEmail;
    private final String sourceRepoHomepage;
    private final String sourceRepoName;
    private final String sourceNamespace;
    private final String sourceRepoUrl;
    private final String sourceRepoSshUrl;
    private final String sourceRepoHttpUrl;
    private final String mergeCommitSha;
    private final String mergeRequestTitle;
    private final String mergeRequestDescription;
    private final Integer mergeRequestId;
    private final Integer mergeRequestIid;
    private final String mergeRequestState;
    private final String mergedByUser;
    private final String mergeRequestAssignee;
    private final Integer mergeRequestTargetProjectId;
    private final List<String> mergeRequestLabels;
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
    private final String ref;
    private final String beforeSha;
    private final String isTag;
    private final String sha;
    private final String status;
    private final String stages;
    private final String createdAt;
    private final String finishedAt;
    private final String buildDuration;
    private final String commentAuthor;

    @GeneratePojoBuilder(withFactoryMethod = "*")
    CauseData(
            ActionType actionType,
            Integer sourceProjectId,
            Integer targetProjectId,
            Integer projectId,
            String branch,
            String sourceBranch,
            String userName,
            String userUsername,
            String userEmail,
            String sourceRepoHomepage,
            String sourceRepoName,
            String sourceNamespace,
            String sourceRepoUrl,
            String sourceRepoSshUrl,
            String sourceRepoHttpUrl,
            String mergeCommitSha,
            String mergeRequestTitle,
            String mergeRequestDescription,
            Integer mergeRequestId,
            Integer mergeRequestIid,
            Integer mergeRequestTargetProjectId,
            List<String> mergeRequestLabels,
            String targetBranch,
            String targetRepoName,
            String targetNamespace,
            String targetRepoSshUrl,
            String targetRepoHttpUrl,
            String triggeredByUser,
            String before,
            String after,
            String lastCommit,
            String targetProjectUrl,
            String triggerPhrase,
            String mergeRequestState,
            String mergedByUser,
            String mergeRequestAssignee,
            String ref,
            String isTag,
            String sha,
            String beforeSha,
            String status,
            String stages,
            String createdAt,
            String finishedAt,
            String buildDuration,
            String commentAuthor) {
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null.");
        this.sourceProjectId = Objects.requireNonNull(sourceProjectId, "sourceProjectId must not be null.");
        this.targetProjectId = Objects.requireNonNull(targetProjectId, "targetProjectId must not be null.");
        this.projectId = projectId == null ? 0 : projectId;
        this.branch = Objects.requireNonNull(branch, "branch must not be null.");
        this.sourceBranch = Objects.requireNonNull(sourceBranch, "sourceBranch must not be null.");
        this.userName = Objects.requireNonNull(userName, "userName must not be null.");
        this.userUsername = userUsername == null ? "" : userUsername;
        this.userEmail = userEmail == null ? "" : userEmail;
        this.sourceRepoHomepage = sourceRepoHomepage == null ? "" : sourceRepoHomepage;
        this.sourceRepoName = Objects.requireNonNull(sourceRepoName, "sourceRepoName must not be null.");
        this.sourceNamespace = Objects.requireNonNull(sourceNamespace, "sourceNamespace must not be null.");
        this.sourceRepoUrl = sourceRepoUrl == null ? sourceRepoSshUrl : sourceRepoUrl;
        this.sourceRepoSshUrl = Objects.requireNonNull(sourceRepoSshUrl, "sourceRepoSshUrl must not be null.");
        this.sourceRepoHttpUrl = Objects.requireNonNull(sourceRepoHttpUrl, "sourceRepoHttpUrl must not be null.");
        this.mergeCommitSha = mergeCommitSha == null ? "" : mergeCommitSha;
        this.mergeRequestTitle = Objects.requireNonNull(mergeRequestTitle, "mergeRequestTitle must not be null.");
        this.mergeRequestDescription = mergeRequestDescription == null ? "" : mergeRequestDescription;
        this.mergeRequestId = mergeRequestId;
        this.mergeRequestIid = mergeRequestIid;
        this.mergeRequestState = mergeRequestState == null ? "" : mergeRequestState;
        this.mergedByUser = mergedByUser == null ? "" : mergedByUser;
        this.mergeRequestAssignee = mergeRequestAssignee == null ? "" : mergeRequestAssignee;
        this.mergeRequestTargetProjectId = mergeRequestTargetProjectId;
        this.mergeRequestLabels = mergeRequestLabels == null ? Collections.emptyList() : mergeRequestLabels;
        this.targetBranch = Objects.requireNonNull(targetBranch, "targetBranch must not be null.");
        this.targetRepoName = Objects.requireNonNull(targetRepoName, "targetRepoName must not be null.");
        this.targetNamespace = Objects.requireNonNull(targetNamespace, "targetNamespace must not be null.");
        this.targetRepoSshUrl = Objects.requireNonNull(targetRepoSshUrl, "targetRepoSshUrl must not be null.");
        this.targetRepoHttpUrl = Objects.requireNonNull(targetRepoHttpUrl, "targetRepoHttpUrl must not be null.");
        this.triggeredByUser = Objects.requireNonNull(triggeredByUser, "triggeredByUser must not be null.");
        this.before = before == null ? "" : before;
        this.after = after == null ? "" : after;
        this.lastCommit = Objects.requireNonNull(lastCommit, "lastCommit must not be null");
        this.targetProjectUrl = targetProjectUrl;
        this.triggerPhrase = triggerPhrase;
        this.ref = ref;
        this.isTag = isTag;
        this.sha = sha;
        this.beforeSha = beforeSha;
        this.status = status;
        this.stages = stages;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
        this.buildDuration = buildDuration;
        this.commentAuthor = commentAuthor;
    }

    @Exported
    public Map<String, String> getBuildVariables() {
        MapWrapper<String, String> variables = new MapWrapper<>(new HashMap<String, String>());
        variables.put("gitlabProjectId", projectId == null ? "" : projectId.toString());
        variables.put("gitlabBranch", branch);
        variables.put("gitlabSourceBranch", sourceBranch);
        variables.put("gitlabActionType", actionType.name());
        variables.put("gitlabUserName", userName);
        variables.put("gitlabUserUsername", userUsername == null ? "" : userUsername);
        variables.put("gitlabUserEmail", userEmail);
        variables.put("gitlabSourceRepoHomepage", sourceRepoHomepage);
        variables.put("gitlabSourceRepoName", sourceRepoName);
        variables.put("gitlabSourceNamespace", sourceNamespace);
        variables.put("gitlabSourceRepoURL", sourceRepoUrl);
        variables.put("gitlabSourceRepoSshUrl", sourceRepoSshUrl);
        variables.put("gitlabSourceRepoHttpUrl", sourceRepoHttpUrl);
        variables.putIfNotNull("gitlabMergeCommitSha", mergeCommitSha);
        variables.put("gitlabMergeRequestTitle", mergeRequestTitle);
        variables.put("gitlabMergeRequestDescription", mergeRequestDescription);
        variables.put("gitlabMergeRequestId", mergeRequestId == null ? "" : mergeRequestId.toString());
        variables.put("gitlabMergeRequestIid", mergeRequestIid == null ? "" : mergeRequestIid.toString());
        variables.put(
                "gitlabMergeRequestTargetProjectId",
                mergeRequestTargetProjectId == null ? "" : mergeRequestTargetProjectId.toString());
        variables.put("gitlabMergeRequestLabels", StringUtils.join(mergeRequestLabels, ","));
        variables.put("gitlabMergeRequestLastCommit", lastCommit);
        variables.putIfNotNull("gitlabMergeRequestState", mergeRequestState);
        variables.putIfNotNull("gitlabMergedByUser", mergedByUser);
        variables.putIfNotNull("gitlabMergeRequestAssignee", mergeRequestAssignee);
        variables.put("gitlabTargetBranch", targetBranch);
        variables.put("gitlabTargetRepoName", targetRepoName);
        variables.put("gitlabTargetNamespace", targetNamespace);
        variables.put("gitlabTargetRepoSshUrl", targetRepoSshUrl);
        variables.put("gitlabTargetRepoHttpUrl", targetRepoHttpUrl);
        variables.put("gitlabBefore", before);
        variables.put("gitlabAfter", after);
        variables.putIfNotNull("gitlabCommentAuthor", commentAuthor);
        variables.put("ref", ref);
        variables.put("beforeSha", beforeSha);
        variables.put("isTag", isTag);
        variables.put("sha", sha);
        variables.put("status", status);
        variables.put("stages", stages);
        variables.put("createdAt", createdAt);
        variables.put("finishedAt", finishedAt);
        variables.put("duration", buildDuration);
        variables.putIfNotNull("gitlabTriggerPhrase", triggerPhrase);
        return variables;
    }

    @Exported
    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    @Exported
    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    @Exported
    public String getBranch() {
        return branch;
    }

    @Exported
    public String getSourceBranch() {
        return sourceBranch;
    }

    @Exported
    public ActionType getActionType() {
        return actionType;
    }

    @Exported
    public String getUserName() {
        return userName;
    }

    @Exported
    public String getUserUsername() {
        return userUsername;
    }

    @Exported
    public String getUserEmail() {
        return userEmail;
    }

    @Exported
    public String getSourceRepoHomepage() {
        return sourceRepoHomepage;
    }

    @Exported
    public String getSourceRepoName() {
        return sourceRepoName;
    }

    @Exported
    public String getSourceNamespace() {
        return sourceNamespace;
    }

    @Exported
    public String getSourceRepoUrl() {
        return sourceRepoUrl;
    }

    @Exported
    public String getSourceRepoSshUrl() {
        return sourceRepoSshUrl;
    }

    @Exported
    public String getSourceRepoHttpUrl() {
        return sourceRepoHttpUrl;
    }

    @Exported
    public String getMergeRequestTitle() {
        return mergeRequestTitle;
    }

    @Exported
    public String getMergeCommitSha() {
        return mergeCommitSha;
    }

    @Exported
    public String getMergeRequestDescription() {
        return mergeRequestDescription;
    }

    @Exported
    public Integer getMergeRequestId() {
        return mergeRequestId;
    }

    @Exported
    public Integer getMergeRequestIid() {
        return mergeRequestIid;
    }

    @Exported
    public Integer getMergeRequestTargetProjectId() {
        return mergeRequestTargetProjectId;
    }

    @Exported
    public List<String> getMergeRequestLabels() {
        return mergeRequestLabels;
    }

    @Exported
    public String getTargetBranch() {
        return targetBranch;
    }

    @Exported
    public String getTargetRepoName() {
        return targetRepoName;
    }

    @Exported
    public String getTargetNamespace() {
        return targetNamespace;
    }

    @Exported
    public String getTargetRepoSshUrl() {
        return targetRepoSshUrl;
    }

    @Exported
    public String getTargetRepoHttpUrl() {
        return targetRepoHttpUrl;
    }

    @Exported
    public String getTriggeredByUser() {
        return triggeredByUser;
    }

    @Exported
    public String getBefore() {
        return before;
    }

    @Exported
    public String getAfter() {
        return after;
    }

    @Exported
    public String getLastCommit() {
        return lastCommit;
    }

    @Exported
    public String getTargetProjectUrl() {
        return targetProjectUrl;
    }

    @Exported
    public String getRef() {
        return ref;
    }

    @Exported
    public String getIsTag() {
        return isTag;
    }

    @Exported
    public String getSha() {
        return sha;
    }

    @Exported
    public String getBeforeSha() {
        return beforeSha;
    }

    @Exported
    public String getStatus() {
        return status;
    }

    @Exported
    public String getStages() {
        return stages;
    }

    @Exported
    public String getCreatedAt() {
        return createdAt;
    }

    @Exported
    public String getFinishedAt() {
        return finishedAt;
    }

    @Exported
    public String getBuildDuration() {
        return buildDuration;
    }

    @Exported
    public String getCommentAuthor() {
        return commentAuthor;
    }

    String getShortDescription() {
        return actionType.getShortDescription(this);
    }

    @Exported
    public String getMergeRequestState() {
        return mergeRequestState;
    }

    @Exported
    public String getMergedByUser() {
        return mergedByUser;
    }

    @Exported
    public String getMergeRequestAssignee() {
        return mergeRequestAssignee;
    }

    @Exported
    public MergeRequest getMergeRequest() {
        if (mergeRequestId == null) {
            return null;
        }

        return new MergeRequest(
                mergeRequestId,
                mergeRequestIid,
                mergeCommitSha,
                sourceBranch,
                targetBranch,
                mergeRequestTitle,
                sourceProjectId,
                targetProjectId,
                mergeRequestDescription,
                mergeRequestState);
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
                .append(userUsername, causeData.userUsername)
                .append(userEmail, causeData.userEmail)
                .append(sourceRepoHomepage, causeData.sourceRepoHomepage)
                .append(sourceRepoName, causeData.sourceRepoName)
                .append(sourceNamespace, causeData.sourceNamespace)
                .append(sourceRepoUrl, causeData.sourceRepoUrl)
                .append(sourceRepoSshUrl, causeData.sourceRepoSshUrl)
                .append(sourceRepoHttpUrl, causeData.sourceRepoHttpUrl)
                .append(mergeCommitSha, causeData.mergeCommitSha)
                .append(mergeRequestTitle, causeData.mergeRequestTitle)
                .append(mergeRequestDescription, causeData.mergeRequestDescription)
                .append(mergeRequestId, causeData.mergeRequestId)
                .append(mergeRequestIid, causeData.mergeRequestIid)
                .append(mergeRequestState, causeData.mergeRequestState)
                .append(mergedByUser, causeData.mergedByUser)
                .append(mergeRequestAssignee, causeData.mergeRequestAssignee)
                .append(mergeRequestTargetProjectId, causeData.mergeRequestTargetProjectId)
                .append(mergeRequestLabels, causeData.mergeRequestLabels)
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
                .append(ref, causeData.getRef())
                .append(isTag, causeData.getIsTag())
                .append(sha, causeData.getSha())
                .append(beforeSha, causeData.getBeforeSha())
                .append(status, causeData.getStatus())
                .append(stages, causeData.getStages())
                .append(createdAt, causeData.getCreatedAt())
                .append(finishedAt, causeData.getFinishedAt())
                .append(buildDuration, causeData.getBuildDuration())
                .append(commentAuthor, causeData.getCommentAuthor())
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
                .append(userUsername)
                .append(userEmail)
                .append(sourceRepoHomepage)
                .append(sourceRepoName)
                .append(sourceNamespace)
                .append(sourceRepoUrl)
                .append(sourceRepoSshUrl)
                .append(sourceRepoHttpUrl)
                .append(mergeCommitSha)
                .append(mergeRequestTitle)
                .append(mergeRequestDescription)
                .append(mergeRequestId)
                .append(mergeRequestIid)
                .append(mergeRequestState)
                .append(mergedByUser)
                .append(mergeRequestAssignee)
                .append(mergeRequestTargetProjectId)
                .append(mergeRequestLabels)
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
                .append(ref)
                .append(isTag)
                .append(sha)
                .append(beforeSha)
                .append(status)
                .append(stages)
                .append(createdAt)
                .append(finishedAt)
                .append(buildDuration)
                .append(commentAuthor)
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
                .append("userUsername", userUsername)
                .append("userEmail", userEmail)
                .append("sourceRepoHomepage", sourceRepoHomepage)
                .append("sourceRepoName", sourceRepoName)
                .append("sourceNamespace", sourceNamespace)
                .append("sourceRepoUrl", sourceRepoUrl)
                .append("sourceRepoSshUrl", sourceRepoSshUrl)
                .append("sourceRepoHttpUrl", sourceRepoHttpUrl)
                .append("mergeCommitSha", mergeCommitSha)
                .append("mergeRequestTitle", mergeRequestTitle)
                .append("mergeRequestDescription", mergeRequestDescription)
                .append("mergeRequestId", mergeRequestId)
                .append("mergeRequestIid", mergeRequestIid)
                .append("mergeRequestState", mergeRequestState)
                .append("mergedByUser", mergedByUser)
                .append("mergeRequestAssignee", mergeRequestAssignee)
                .append("mergeRequestTargetProjectId", mergeRequestTargetProjectId)
                .append("mergeRequestLabels", mergeRequestLabels)
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
                .append("ref", ref)
                .append("isTag", isTag)
                .append("sha", sha)
                .append("beforeSha", beforeSha)
                .append("status", status)
                .append("stages", stages)
                .append("createdAt", createdAt)
                .append("finishedAt", finishedAt)
                .append("duration", buildDuration)
                .append("commentAuthor", commentAuthor)
                .toString();
    }

    public enum ActionType {
        PUSH {
            @Override
            String getShortDescription(CauseData data) {
                return getShortDescriptionPush(data);
            }
        },
        TAG_PUSH {
            @Override
            String getShortDescription(CauseData data) {
                return getShortDescriptionPush(data);
            }
        },
        MERGE {
            @Override
            String getShortDescription(CauseData data) {
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch())
                        ? ""
                        : data.getSourceNamespace() + "/";
                if (Jenkins.getActiveInstance().getMarkupFormatter() instanceof EscapedMarkupFormatter
                        || data.getTargetProjectUrl() == null) {
                    return Messages.GitLabWebHookCause_ShortDescription_MergeRequestHook_plain(
                            String.valueOf(data.getMergeRequestIid()),
                            forkNamespace + data.getSourceBranch(),
                            data.getTargetBranch());
                } else {
                    return Messages.GitLabWebHookCause_ShortDescription_MergeRequestHook_html(
                            String.valueOf(data.getMergeRequestIid()),
                            forkNamespace + data.getSourceBranch(),
                            data.getTargetBranch(),
                            data.getTargetProjectUrl());
                }
            }
        },
        NOTE {
            @Override
            String getShortDescription(CauseData data) {
                String triggeredBy = data.getTriggeredByUser();
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch())
                        ? ""
                        : data.getSourceNamespace() + "/";
                if (Jenkins.getActiveInstance().getMarkupFormatter() instanceof EscapedMarkupFormatter
                        || data.getTargetProjectUrl() == null) {
                    return Messages.GitLabWebHookCause_ShortDescription_NoteHook_plain(
                            triggeredBy,
                            String.valueOf(data.getMergeRequestIid()),
                            forkNamespace + data.getSourceBranch(),
                            data.getTargetBranch());
                } else {
                    return Messages.GitLabWebHookCause_ShortDescription_NoteHook_html(
                            triggeredBy,
                            String.valueOf(data.getMergeRequestIid()),
                            forkNamespace + data.getSourceBranch(),
                            data.getTargetBranch(),
                            data.getTargetProjectUrl());
                }
            }
        },
        PIPELINE {
            @Override
            String getShortDescription(CauseData data) {
                String getStatus = data.getStatus();
                if (getStatus == null) {
                    return Messages.GitLabWebHookCause_ShortDescription_PipelineHook_noStatus();
                } else {
                    return Messages.GitLabWebHookCause_ShortDescription_PipelineHook(getStatus);
                }
            }
        };

        private static String getShortDescriptionPush(CauseData data) {
            String pushedBy = data.getTriggeredByUser();
            if (pushedBy == null) {
                return Messages.GitLabWebHookCause_ShortDescription_PushHook_noUser();
            } else {
                return Messages.GitLabWebHookCause_ShortDescription_PushHook(pushedBy);
            }
        }

        abstract String getShortDescription(CauseData data);
    }

    private static class MapWrapper<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> map;

        MapWrapper(Map<K, V> map) {
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

        void putIfNotNull(K key, V value) {
            if (value != null) {
                map.put(key, value);
            }
        }
    }
}
