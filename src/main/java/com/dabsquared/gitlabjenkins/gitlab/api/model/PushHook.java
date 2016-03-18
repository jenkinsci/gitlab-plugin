package com.dabsquared.gitlabjenkins.gitlab.api.model;

import com.google.common.base.Optional;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Collections;
import java.util.List;

/**
 * @author Robin Müller
 */
public class PushHook extends WebHook {

    private final String before;
    private final String after;
    private final String ref;
    private final Integer userId;
    private final String userName;
    private final String userEmail;
    private final String userAvatar;
    private final Integer projectId;
    private final Project project;
    private final Repository repository;
    private final List<Commit> commits;
    private final Integer totalCommitsCount;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public PushHook(String objectKind, String before, String after, String ref, Integer userId, String userName, String userEmail, String userAvatar,
                    Integer projectId, Project project, Repository repository, List<Commit> commits, Integer totalCommitsCount) {
        super(objectKind);
        this.before = before;
        this.after = after;
        this.ref = ref;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userAvatar = userAvatar;
        this.projectId = projectId;
        this.project = project;
        this.repository = repository;
        this.commits = commits;
        this.totalCommitsCount = totalCommitsCount;
    }

    PushHook() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Optional<Project> optProject() {
        return Optional.fromNullable(project);
    }

    public Optional<Integer> optProjectId() {
        return Optional.fromNullable(projectId);
    }

    public Optional<String> optUserAvatar() {
        return Optional.fromNullable(userAvatar);
    }

    public Optional<String> optUserEmail() {
        return Optional.fromNullable(userEmail);
    }

    public Optional<String> optUserName() {
        return Optional.fromNullable(userName);
    }

    public Optional<Integer> optUserId() {
        return Optional.fromNullable(userId);
    }

    public Optional<String> optRef() {
        return Optional.fromNullable(ref);
    }

    public Optional<String> optAfter() {
        return Optional.fromNullable(after);
    }

    public Optional<String> optBefore() {
        return Optional.fromNullable(before);
    }

    public Project getProject() {
        return project == null ? new Project() : project;
    }

    public Optional<Repository> optRepository() {
        return Optional.fromNullable(repository);
    }

    public Repository getRepository() {
        return repository == null ? new Repository() : repository;
    }

    public Optional<List<Commit>> optCommits() {
        return Optional.fromNullable(commits);
    }

    public List<Commit> getCommits() {
        return commits == null ? Collections.<Commit>emptyList() : commits;
    }

    public Optional<Integer> optTotalCommitsCount() {
        return Optional.fromNullable(totalCommitsCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PushHook pushHook = (PushHook) o;
        return new EqualsBuilder()
                .append(before, pushHook.before)
                .append(after, pushHook.after)
                .append(ref, pushHook.ref)
                .append(userId, pushHook.userId)
                .append(userName, pushHook.userName)
                .append(userEmail, pushHook.userEmail)
                .append(userAvatar, pushHook.userAvatar)
                .append(projectId, pushHook.projectId)
                .append(project, pushHook.project)
                .append(repository, pushHook.repository)
                .append(commits, pushHook.commits)
                .append(totalCommitsCount, pushHook.totalCommitsCount)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(before)
                .append(after)
                .append(ref)
                .append(userId)
                .append(userName)
                .append(userEmail)
                .append(userAvatar)
                .append(projectId)
                .append(project)
                .append(repository)
                .append(commits)
                .append(totalCommitsCount)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("before", before)
                .append("after", after)
                .append("ref", ref)
                .append("userId", userId)
                .append("userName", userName)
                .append("userEmail", userEmail)
                .append("userAvatar", userAvatar)
                .append("projectId", projectId)
                .append("project", project)
                .append("repository", repository)
                .append("commits", commits)
                .append("totalCommitsCount", totalCommitsCount)
                .toString();
    }
}
