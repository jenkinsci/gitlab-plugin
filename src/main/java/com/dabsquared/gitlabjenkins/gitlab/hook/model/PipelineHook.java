package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;


/**
 * @author Milena Zachow
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class PipelineHook extends WebHook {

    private User user;
    public Integer projectId;
    private List<Commit> commits;
    private Project project;
    private PipelineEventObjectAttributes objectAttributes;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public PipelineEventObjectAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(PipelineEventObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineHook that = (PipelineHook) o;
        return new EqualsBuilder()
                .append(user, that.user)
                .append(project, that.project)
                .append(projectId, that.projectId)
                .append(commits, that.commits)
                .append(objectAttributes, that.objectAttributes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(user)
                .append(projectId)
                .append(project)
                .append(commits)
                .append(objectAttributes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("project", project)
                .append("projectId", projectId)
                .append("objectAttributes", objectAttributes)
                .append("commits", commits)
                .toString();
    }
}
