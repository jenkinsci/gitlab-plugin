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
public class MergeRequestHook extends WebHook {

    /*
        "user": {...},
        "assignee": {...},
        "project": {...},
        "object_attributes": {...},
        "labels": [{...}],
        "changes": {...}
    */
    private User user;
    private User assignee;
    private MergeRequestObjectAttributes objectAttributes;
    private List<MergeRequestLabel> labels;
    private MergeRequestChanges changes;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public MergeRequestObjectAttributes getObjectAttributes() {
        return objectAttributes;
    }

    public void setObjectAttributes(MergeRequestObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }

    public List<MergeRequestLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<MergeRequestLabel> labels) {
        this.labels = labels;
    }

    public MergeRequestChanges getChanges() {
        return changes;
    }

    public void setChanges(MergeRequestChanges changes) {
        this.changes = changes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestHook that = (MergeRequestHook) o;
        return new EqualsBuilder()
                .append(user, that.user)
                .append(assignee, that.assignee)
                .append(getProject(), that.getProject())
                .append(objectAttributes, that.objectAttributes)
                .append(labels, that.labels)
                .append(changes, that.changes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(user)
                .append(assignee)
                .append(getProject())
                .append(objectAttributes)
                .append(labels)
                .append(changes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("assignee", assignee)
                .append("project", getProject())
                .append("objectAttributes", objectAttributes)
                .append("labels", labels)
                .append("changes", changes)
                .toString();
    }
}
