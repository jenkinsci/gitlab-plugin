package com.dabsquared.gitlabjenkins.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Robin Müller
 */
public class Commit {

    private final String id;
    private final String message;
    private final Date timestamp;
    private final String url;
    private final User author;
    private final List<String> added;
    private final List<String> modified;
    private final List<String> removed;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public Commit(String id, String message, Date timestamp, String url, User author, List<String> added, List<String> modified, List<String> removed) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.url = url;
        this.author = author;
        this.added = added == null ? Collections.<String>emptyList() : added;
        this.modified = modified == null ? Collections.<String>emptyList() : modified;
        this.removed = removed == null ? Collections.<String>emptyList() : removed;
    }

    Commit() {
        this(null, null, null, null, null, null, null, null);
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }

    public User getAuthor() {
        return author;
    }

    public List<String> getAdded() {
        return added;
    }

    public List<String> getModified() {
        return modified;
    }

    public List<String> getRemoved() {
        return removed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Commit commit = (Commit) o;
        return new EqualsBuilder()
                .append(id, commit.id)
                .append(message, commit.message)
                .append(timestamp, commit.timestamp)
                .append(url, commit.url)
                .append(author, commit.author)
                .append(added, commit.added)
                .append(modified, commit.modified)
                .append(removed, commit.removed)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(message)
                .append(timestamp)
                .append(url)
                .append(author)
                .append(added)
                .append(modified)
                .append(removed)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("message", message)
                .append("timestamp", timestamp)
                .append("url", url)
                .append("author", author)
                .append("added", added)
                .append("modified", modified)
                .append("removed", removed)
                .toString();
    }
}
