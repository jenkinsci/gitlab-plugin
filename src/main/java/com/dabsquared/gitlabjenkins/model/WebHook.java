package com.dabsquared.gitlabjenkins.model;

import com.google.common.base.Optional;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public abstract class WebHook {

    private final String objectKind;

    protected WebHook(String objectKind) {
        this.objectKind = objectKind;
    }

    public Optional<String> optObjectKind() {
        return Optional.fromNullable(objectKind);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebHook webHook = (WebHook) o;
        return new EqualsBuilder()
                .append(objectKind, webHook.objectKind)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(objectKind)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("objectKind", objectKind)
                .toString();
    }
}
