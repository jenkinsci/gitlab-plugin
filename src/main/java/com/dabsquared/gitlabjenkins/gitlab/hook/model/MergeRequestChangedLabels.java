package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import java.util.List;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Anton Johansson
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestChangedLabels {

    /*
        "previous": [{...}],
        "current": [{...}]
    */
    private List<MergeRequestLabel> previous;
    private List<MergeRequestLabel> current;

    public List<MergeRequestLabel> getPrevious() {
        return previous;
    }

    public void setPrevious(List<MergeRequestLabel> previous) {
        this.previous = previous;
    }

    public List<MergeRequestLabel> getCurrent() {
        return current;
    }

    public void setCurrent(List<MergeRequestLabel> current) {
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
        MergeRequestChangedLabels that = (MergeRequestChangedLabels) o;
        return new EqualsBuilder()
                .append(previous, that.previous)
                .append(current, that.current)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(previous).append(current).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("previous", previous)
                .append("current", current)
                .toString();
    }
}
