package com.dabsquared.gitlabjenkins.gitlab.api.model;


import com.google.common.base.Optional;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public class MergeRequestHook extends WebHook {

    private final User user;
    private final Project project;
    private final ObjectAttributes objectAttributes;
    private final Repository repository;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public MergeRequestHook(String objectKind, User user, Project project, ObjectAttributes objectAttributes, Repository repository) {
        super(objectKind);
        this.user = user;
        this.project = project;
        this.objectAttributes = objectAttributes;
        this.repository = repository;
    }

    MergeRequestHook() {
        this(null, null, null, null, null);
    }

    public Optional<User> optUser() {
        return Optional.fromNullable(user);
    }

    public User getUser() {
        return user == null ? new User() : user;
    }

    public Optional<Project> optProject() {
        return Optional.fromNullable(project);
    }

    public Project getProject() {
        return project == null ? new Project() : project;
    }

    public Optional<ObjectAttributes> optObjectAttributes() {
        return Optional.fromNullable(objectAttributes);
    }

    public ObjectAttributes getObjectAttributes() {
        return objectAttributes == null ? new ObjectAttributes() : objectAttributes;
    }

    public Optional<Repository> optRepository() {
        return Optional.fromNullable(repository);
    }

    public Repository getRepository() {
        return repository == null ? new Repository() : repository;
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
                .append(project, that.project)
                .append(objectAttributes, that.objectAttributes)
                .append(repository, that.repository)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(user)
                .append(project)
                .append(objectAttributes)
                .append(repository)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("project", project)
                .append("objectAttributes", objectAttributes)
                .append("repository", repository)
                .toString();
    }
}
