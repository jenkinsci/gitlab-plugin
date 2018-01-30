package com.dabsquared.gitlabjenkins.gitlab.hook.model;


import com.dabsquared.gitlabjenkins.gitlab.api.model.Label;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Nikolay Ustinov
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class NoteHook extends WebHook {

    private User user;
    private Project project;
    private MergeRequestObjectAttributes mergeRequest;
    private NoteObjectAttributes objectAttributes;
    private List<Label> labels;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public NoteObjectAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(NoteObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }

    public MergeRequestObjectAttributes getMergeRequest() {
        return mergeRequest;
    }

    public void setMergeRequest(MergeRequestObjectAttributes mergeRequest) {
        this.mergeRequest = mergeRequest;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoteHook that = (NoteHook) o;
        return new EqualsBuilder()
            .append(user, that.user)
            .append(project, that.project)
            .append(objectAttributes, that.objectAttributes)
            .append(mergeRequest, that.mergeRequest)
            .append(labels, that.labels)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(user)
            .append(project)
            .append(objectAttributes)
            .append(mergeRequest)
            .append(labels)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("user", user)
            .append("project", project)
            .append("objectAttributes", objectAttributes)
            .append("mergeRequest", mergeRequest)
            .append("labels", labels)
            .toString();
    }
}
