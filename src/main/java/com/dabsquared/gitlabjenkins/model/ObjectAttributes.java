package com.dabsquared.gitlabjenkins.model;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
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
        this.source = source;
        this.target = target;
        this.lastCommit = lastCommit;
        this.mergeStatus = mergeStatus;
        this.url = url;
        this.action = action;
        this.workInProgress = workInProgress;
    }

    ObjectAttributes() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
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

    public Optional<Integer> optSourceProjectId() {
        return Optional.fromNullable(sourceProjectId);
    }

    public Optional<Integer> optTargetProjectId() {
        return Optional.fromNullable(targetProjectId);
    }

    public Optional<Integer> optAuthorId() {
        return Optional.fromNullable(authorId);
    }

    public Optional<Integer> optAssigneeId() {
        return Optional.fromNullable(assigneeId);
    }

    public Optional<String> optTitle() {
        return Optional.fromNullable(title);
    }

    public Optional<Date> optCreatedAt() {
        return Optional.fromNullable(createdAt);
    }

    public Optional<Date> optUpdatedAt() {
        return Optional.fromNullable(updatedAt);
    }

    public Optional<State> optState() {
        return Optional.fromNullable(state);
    }

    public Optional<String> optDescription() {
        return Optional.fromNullable(description);
    }

    public Optional<Project> optSource() {
        return Optional.fromNullable(source);
    }

    public Project getSource() {
        return source == null ? new Project() : source;
    }

    public Optional<Project> optTarget() {
        return Optional.fromNullable(target);
    }

    public Project getTarget() {
        return target == null ? new Project() : target;
    }

    public Optional<Commit> optLastCommit() {
        return Optional.fromNullable(lastCommit);
    }

    public Commit getLastCommit() {
        return lastCommit == null ? new Commit() : lastCommit;
    }

    public Optional<String> optMergeStatus() {
        return Optional.fromNullable(mergeStatus);
    }

    public Optional<String> optUrl() {
        return Optional.fromNullable(url);
    }

    public Optional<Action> optAction() {
        return Optional.fromNullable(action);
    }

    public Optional<Boolean> optWorkInProgress() {
        return Optional.fromNullable(workInProgress);
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
