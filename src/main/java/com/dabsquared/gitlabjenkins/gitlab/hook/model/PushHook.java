package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class PushHook extends WebHook {

    private String before;
    private String after;
    private String ref;
    private Integer userId;
    private String userName;
    private String userUsername;
    private String userEmail;
    private String userAvatar;
    private Integer projectId;
    private Project project;
    private List<Commit> commits;
    private Integer totalCommitsCount;

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    public Integer getTotalCommitsCount() {
        return totalCommitsCount;
    }

    public void setTotalCommitsCount(Integer totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
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
                .append("commits", commits)
                .append("totalCommitsCount", totalCommitsCount)
                .toString();
    }
}
