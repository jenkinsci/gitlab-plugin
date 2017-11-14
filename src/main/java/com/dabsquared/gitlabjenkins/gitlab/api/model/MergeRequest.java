package com.dabsquared.gitlabjenkins.gitlab.api.model;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequest {
    private Integer id;
    private Integer iid;
    private String sourceBranch;
    private String targetBranch;
    private Integer projectId;
    private String title;
    private State state;
    private Integer upvotes;
    private Integer downvotes;
    private User author;
    private User assignee;
    private Integer sourceProjectId;
    private Integer targetProjectId;
    private List<String> labels;
    private String description;
    private Boolean workInProgress;
    private Boolean mergeWhenBuildSucceeds;
    private String mergeStatus;

    public MergeRequest() { /* default-constructor for Resteasy-based-api-proxies */ }

    public MergeRequest(int id, int iid, String sourceBranch, String targetBranch, String title,
                        int sourceProjectId, int targetProjectId,
                        String description, String mergeStatus) {
        this.id = id;
        this.iid= iid;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.title = title;
        this.sourceProjectId = sourceProjectId;
        this.projectId = targetProjectId;
        this.description = description;
        this.mergeStatus = mergeStatus;
    }

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

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public Integer getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(Integer downvotes) {
        this.downvotes = downvotes;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
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

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getWorkInProgress() {
        return workInProgress;
    }

    public void setWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
    }

    public Boolean getMergeWhenBuildSucceeds() {
        return mergeWhenBuildSucceeds;
    }

    public void setMergeWhenBuildSucceeds(Boolean mergeWhenBuildSucceeds) {
        this.mergeWhenBuildSucceeds = mergeWhenBuildSucceeds;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequest that = (MergeRequest) o;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(iid, that.iid)
                .append(sourceBranch, that.sourceBranch)
                .append(targetBranch, that.targetBranch)
                .append(projectId, that.projectId)
                .append(title, that.title)
                .append(state, that.state)
                .append(upvotes, that.upvotes)
                .append(downvotes, that.downvotes)
                .append(author, that.author)
                .append(assignee, that.assignee)
                .append(sourceProjectId, that.sourceProjectId)
                .append(targetProjectId, that.targetProjectId)
                .append(labels, that.labels)
                .append(description, that.description)
                .append(workInProgress, that.workInProgress)
                .append(mergeWhenBuildSucceeds, that.mergeWhenBuildSucceeds)
                .append(mergeStatus, that.mergeStatus)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(iid)
                .append(sourceBranch)
                .append(targetBranch)
                .append(projectId)
                .append(title)
                .append(state)
                .append(upvotes)
                .append(downvotes)
                .append(author)
                .append(assignee)
                .append(sourceProjectId)
                .append(targetProjectId)
                .append(labels)
                .append(description)
                .append(workInProgress)
                .append(mergeWhenBuildSucceeds)
                .append(mergeStatus)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("iid", iid)
                .append("sourceBranch", sourceBranch)
                .append("targetBranch", targetBranch)
                .append("projectId", projectId)
                .append("title", title)
                .append("state", state)
                .append("upvotes", upvotes)
                .append("downvotes", downvotes)
                .append("author", author)
                .append("assignee", assignee)
                .append("sourceProjectId", sourceProjectId)
                .append("targetProjectId", targetProjectId)
                .append("labels", labels)
                .append("description", description)
                .append("workInProgress", workInProgress)
                .append("mergeWhenBuildSucceeds", mergeWhenBuildSucceeds)
                .append("mergeStatus", mergeStatus)
                .toString();
    }
}
