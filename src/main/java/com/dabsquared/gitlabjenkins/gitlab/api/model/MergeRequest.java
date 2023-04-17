package com.dabsquared.gitlabjenkins.gitlab.api.model;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import java.util.List;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author Robin Müller
 */
@ExportedBean
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

    public MergeRequest() {
        /* default-constructor for Resteasy-based-api-proxies */
    }

    public MergeRequest(
            int id,
            int iid,
            String sourceBranch,
            String targetBranch,
            String title,
            int sourceProjectId,
            int targetProjectId,
            String description,
            String mergeStatus) {
        this.id = id;
        this.iid = iid;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.title = title;
        this.sourceProjectId = sourceProjectId;
        this.projectId = targetProjectId;
        this.description = description;
        this.mergeStatus = mergeStatus;
    }

    @Exported
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Exported
    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    @Exported
    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    @Exported
    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    @Exported
    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    @Exported
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Exported
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Exported
    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    @Exported
    public Integer getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(Integer downvotes) {
        this.downvotes = downvotes;
    }

    @Exported
    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    @Exported
    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    @Exported
    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public void setSourceProjectId(Integer sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    @Exported
    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    public void setTargetProjectId(Integer targetProjectId) {
        this.targetProjectId = targetProjectId;
    }

    @Exported
    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    @Exported
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Exported
    public Boolean getWorkInProgress() {
        return workInProgress;
    }

    public void setWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
    }

    @Exported
    public Boolean getMergeWhenBuildSucceeds() {
        return mergeWhenBuildSucceeds;
    }

    public void setMergeWhenBuildSucceeds(Boolean mergeWhenBuildSucceeds) {
        this.mergeWhenBuildSucceeds = mergeWhenBuildSucceeds;
    }

    @Exported
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
