package com.dabsquared.gitlabjenkins.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;

/**
 * @author Robin Müller
 */
public class ObjectAttributes {

    private final Integer id;
    private final Integer iid;
    private final String sourceBranch;
    private final String targetBranch;
    private final Integer sourceProjectId;
    private final Integer targetProjectId;
    private final Integer authorId;
    private final Integer assigneeId;
    private final String title;
    private final Date createdAt;
    private final Date updatedAt;
    private final State state;
    private final String description;
    private final Project source;
    private final Project target;
    private final Commit lastCommit;
    private final String mergeStatus;
    private final String url;
    private final Action action;
    private final Boolean workInProgress;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public ObjectAttributes(Integer id, Integer iid, String sourceBranch, String targetBranch, Integer sourceProjectId, Integer targetProjectId,
                            Integer authorId, Integer assigneeId, String title, Date createdAt, Date updatedAt, State state, String description,
                            Project source, Project target, Commit lastCommit, String mergeStatus, String url, Action action, Boolean workInProgress) {
        this.id = id;
        this.iid = iid;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.sourceProjectId = sourceProjectId;
        this.targetProjectId = targetProjectId;
        this.authorId = authorId;
        this.assigneeId = assigneeId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.state = state;
        this.description = description;
        this.source = source == null ? new Project() : source;
        this.target = target == null ? new Project() : target;
        this.lastCommit = lastCommit == null ? new Commit() : lastCommit;
        this.mergeStatus = mergeStatus;
        this.url = url;
        this.action = action;
        this.workInProgress = workInProgress;
    }

    ObjectAttributes() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Integer getId() {
        return id;
    }

    public Integer getIid() {
        return iid;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public String getTitle() {
        return title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public State getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public Project getSource() {
        return source;
    }

    public Project getTarget() {
        return target;
    }

    public Commit getLastCommit() {
        return lastCommit;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public String getUrl() {
        return url;
    }

    public Action getAction() {
        return action;
    }

    public Boolean getWorkInProgress() {
        return workInProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectAttributes that = (ObjectAttributes) o;
        return new EqualsBuilder()
                .append(workInProgress, that.workInProgress)
                .append(id, that.id)
                .append(iid, that.iid)
                .append(sourceBranch, that.sourceBranch)
                .append(targetBranch, that.targetBranch)
                .append(sourceProjectId, that.sourceProjectId)
                .append(targetProjectId, that.targetProjectId)
                .append(authorId, that.authorId)
                .append(assigneeId, that.assigneeId)
                .append(title, that.title)
                .append(createdAt, that.createdAt)
                .append(updatedAt, that.updatedAt)
                .append(state, that.state)
                .append(description, that.description)
                .append(source, that.source)
                .append(target, that.target)
                .append(lastCommit, that.lastCommit)
                .append(mergeStatus, that.mergeStatus)
                .append(url, that.url)
                .append(action, that.action)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(iid)
                .append(sourceBranch)
                .append(targetBranch)
                .append(sourceProjectId)
                .append(targetProjectId)
                .append(authorId)
                .append(assigneeId)
                .append(title)
                .append(createdAt)
                .append(updatedAt)
                .append(state)
                .append(description)
                .append(source)
                .append(target)
                .append(lastCommit)
                .append(mergeStatus)
                .append(url)
                .append(action)
                .append(workInProgress)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("iid", iid)
                .append("sourceBranch", sourceBranch)
                .append("targetBranch", targetBranch)
                .append("sourceProjectId", sourceProjectId)
                .append("targetProjectId", targetProjectId)
                .append("authorId", authorId)
                .append("assigneeId", assigneeId)
                .append("title", title)
                .append("createdAt", createdAt)
                .append("updatedAt", updatedAt)
                .append("state", state)
                .append("description", description)
                .append("source", source)
                .append("target", target)
                .append("lastCommit", lastCommit)
                .append("mergeStatus", mergeStatus)
                .append("url", url)
                .append("action", action)
                .append("workInProgress", workInProgress)
                .toString();
    }
}
