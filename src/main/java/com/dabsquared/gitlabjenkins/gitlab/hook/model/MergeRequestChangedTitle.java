package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Anton Johansson
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestChangedTitle {

    /*
        "previous": [{...}],
        "current": [{...}]
    */
    private String previous;
    private String current;

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestChangedTitle that = (MergeRequestChangedTitle) o;
        return new EqualsBuilder()
            .append(previous, that.previous)
            .append(current, that.current)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(previous)
            .append(current)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("previous", previous)
            .append("current", current)
            .toString();
    }
}
