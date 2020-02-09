package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;
import java.util.List;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestObjectAttributes {

    private Integer id;
    private Integer iid;
    private String sourceBranch;
    private String targetBranch;
    private Integer sourceProjectId;
    private Integer targetProjectId;
    private Integer authorId;
    private Integer assigneeId;
    private String title;
    private Date createdAt;
    private Date updatedAt;
    private State state;
    private String description;
    private Project source;
    private Project target;
    private Commit lastCommit;
    private String oldrev;
    private String mergeStatus;
    private String url;
    private Action action;
    private Boolean workInProgress;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public void setSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    public void setTargetProjectId(Integer targetProjectId) {
        this.targetProjectId = targetProjectId;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Project getSource() {
        return source;
    }

    public void setSource(Project source) {
        this.source = source;
    }

    public Project getTarget() {
        return target;
    }

    public void setTarget(Project target) {
        this.target = target;
    }

    public Commit getLastCommit() {
        return lastCommit;
    }

    public void setLastCommit(Commit lastCommit) {
        this.lastCommit = lastCommit;
    }

    public String getOldrev() { return oldrev; }

    public void setOldrev(String oldrev) { this.oldrev = oldrev; }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Boolean getWorkInProgress() {
        return workInProgress;
    }

    public void setWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestObjectAttributes that = (MergeRequestObjectAttributes) o;
        return new EqualsBuilder()
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
            .append(oldrev, that.oldrev)
            .append(workInProgress, that.workInProgress)
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
            .append("oldrev", oldrev)
            .append("workInProgress", workInProgress)
            .toString();
    }
}
