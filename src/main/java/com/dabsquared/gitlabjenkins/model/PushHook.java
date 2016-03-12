package com.dabsquared.gitlabjenkins.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
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
        this.project = project == null ? new Project() : project;
        this.repository = repository == null ? new Repository() : repository;
        this.commits = commits == null ? new ArrayList<Commit>() : commits;
        this.totalCommitsCount = totalCommitsCount;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public String getRef() {
        return ref;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public Project getProject() {
        return project;
    }

    public Repository getRepository() {
        return repository;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public Integer getTotalCommitsCount() {
        return totalCommitsCount;
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
