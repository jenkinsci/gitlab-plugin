package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Anton Johansson
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestChanges {

    /*
        "labels": {...}
    */
    private MergeRequestChangedLabels labels;

    public MergeRequestChangedLabels getLabels() {
        return labels;
    }

    public void setLabels(MergeRequestChangedLabels labels) {
        this.labels = labels;
    }

    /*
        "title": {...}
    */
    private MergeRequestChangedTitle title;

    public MergeRequestChangedTitle getTitle() {
        return title;
    }

    public void setTitle(MergeRequestChangedTitle title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestChanges that = (MergeRequestChanges) o;
        return new EqualsBuilder().append(labels, that.labels).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(labels).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("labels", labels).toString();
    }
}
