package com.dabsquared.gitlabjenkins.gitlab.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Robin Müller
 */
public class MergeRequest {

    private final Integer id;
    private final Integer iid;
    private final String sourceBranch;
    private final String targetBranch;
    private final Integer projectId;
    private final String title;
    private final State state;
    private final Integer upvotes;
    private final Integer downvotes;
    private final User author;
    private final User assignee;
    private final Integer sourceProjectId;
    private final Integer targetProjectId;
    private final List<String> labels;
    private final String description;
    private final Boolean workInProgress;
    private final Boolean mergeWhenBuildSucceeds;
    private final String mergeStatus;

    @JsonCreator
    @GeneratePojoBuilder(intoPackage = "*.generated.builder", withFactoryMethod = "*")
    public MergeRequest(@JsonProperty("id") Integer id,
                        @JsonProperty("iid") Integer iid,
                        @JsonProperty("source_branch") String sourceBranch,
                        @JsonProperty("target_branch") String targetBranch,
                        @JsonProperty("project_id") Integer projectId,
                        @JsonProperty("title") String title,
                        @JsonProperty("state") State state,
                        @JsonProperty("upvotes") Integer upvotes,
                        @JsonProperty("downvotes") Integer downvotes,
                        @JsonProperty("author") User author,
                        @JsonProperty("assignee") User assignee,
                        @JsonProperty("source_project_id") Integer sourceProjectId,
                        @JsonProperty("target_project_id") Integer targetProjectId,
                        @JsonProperty("labels") List<String> labels,
                        @JsonProperty("description") String description,
                        @JsonProperty("work_in_progress") Boolean workInProgress,
                        @JsonProperty("merge_when_build_succeeds") Boolean mergeWhenBuildSucceeds,
                        @JsonProperty("merge_status") String mergeStatus) {
        this.id = id;
        this.iid = iid;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.projectId = projectId;
        this.title = title;
        this.state = state;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        this.author = author;
        this.assignee = assignee;
        this.sourceProjectId = sourceProjectId;
        this.targetProjectId = targetProjectId;
        this.labels = labels;
        this.description = description;
        this.workInProgress = workInProgress;
        this.mergeWhenBuildSucceeds = mergeWhenBuildSucceeds;
        this.mergeStatus = mergeStatus;
    }

    public Optional<Integer> optId() {
        return Optional.fromNullable(id);
    }

    public Optional<Integer> optIid() {
        return Optional.fromNullable(iid);
    }

    public Optional<String> optSourceBranch() {
        return Optional.fromNullable(sourceBranch);
    }

    public Optional<String> optTargetBranch() {
        return Optional.fromNullable(targetBranch);
    }

    public Optional<Integer> optProjectId() {
        return Optional.fromNullable(projectId);
    }

    public Optional<String> optTitle() {
        return Optional.fromNullable(title);
    }

    public Optional<State> optState() {
        return Optional.fromNullable(state);
    }

    public Optional<Integer> optUpvotes() {
        return Optional.fromNullable(upvotes);
    }

    public Optional<Integer> optDownvotes() {
        return Optional.fromNullable(downvotes);
    }

    public Optional<User> optAuthor() {
        return Optional.fromNullable(author);
    }

    public User getAuthor() {
        return author == null ? new User() : author;
    }

    public Optional<User> optAssignee() {
        return Optional.fromNullable(assignee);
    }

    public User getAssignee() {
        return assignee == null ? new User() : assignee;
    }

    public Optional<Integer> optSourceProjectId() {
        return Optional.fromNullable(sourceProjectId);
    }

    public Optional<Integer> optTargetProjectId() {
        return Optional.fromNullable(targetProjectId);
    }

    public Optional<List<String>> optLabels() {
        return Optional.fromNullable(labels);
    }

    public Optional<String> optDescription() {
        return Optional.fromNullable(description);
    }

    public Optional<Boolean> optWorkInProgress() {
        return Optional.fromNullable(workInProgress);
    }

    public Optional<Boolean> optMergeWhenBuildSucceeds() {
        return Optional.fromNullable(mergeWhenBuildSucceeds);
    }

    public Optional<String> optMergeStatus() {
        return Optional.fromNullable(mergeStatus);
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
