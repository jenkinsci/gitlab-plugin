package com.dabsquared.gitlabjenkins.model;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.dabsquared.gitlabjenkins.model.User.nullUser;

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
        this.added = added;
        this.modified = modified;
        this.removed = removed;
    }

    Commit() {
        this(null, null, null, null, null, null, null, null);
    }

    public Optional<String> optId() {
        return Optional.fromNullable(id);
    }

    public Optional<String> optMessage() {
        return Optional.fromNullable(message);
    }

    public Optional<Date> optTimestamp() {
        return Optional.fromNullable(timestamp);
    }

    public Optional<String> optUrl() {
        return Optional.fromNullable(url);
    }

    public Optional<User> optAuthor() {
        return Optional.fromNullable(author);
    }

    public User getAuthor() {
        return author == null ? new User() : author;
    }

    public Optional<List<String>> optAdded() {
        return Optional.fromNullable(added);
    }

    public List<String> getAdded() {
        return added == null ? Collections.<String>emptyList() : added;
    }

    public Optional<List<String>> optModified() {
        return Optional.fromNullable(modified);
    }

    public List<String> getModified() {
        return modified == null ? Collections.<String>emptyList() : modified;
    }

    public Optional<List<String>> optRemoved() {
        return Optional.fromNullable(removed);
    }

    public List<String> getRemoved() {
        return removed == null ? Collections.<String>emptyList() : removed;
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
