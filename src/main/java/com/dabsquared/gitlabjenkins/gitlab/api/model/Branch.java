package com.dabsquared.gitlabjenkins.gitlab.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public class Branch {

    private final String name;
    private final Boolean protectedBranch;
    private final Commit commit;

    @JsonCreator
    @GeneratePojoBuilder(intoPackage = "*.generated.builder", withFactoryMethod = "*")
    public Branch(@JsonProperty("name") String name,
                  @JsonProperty("protected") Boolean protectedBranch,
                  @JsonProperty("commit") Commit commit) {
        this.name = name;
        this.protectedBranch = protectedBranch;
        this.commit = commit;
    }

    public Optional<String> optName() {
        return Optional.fromNullable(name);
    }

    public Optional<Boolean> optProtectedBranch() {
        return Optional.fromNullable(protectedBranch);
    }

    public Optional<Commit> optCommit() {
        return Optional.fromNullable(commit);
    }

    public Commit getCommit() {
        return commit == null ? new Commit() : commit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Branch branch = (Branch) o;
        return new EqualsBuilder()
                .append(name, branch.name)
                .append(protectedBranch, branch.protectedBranch)
                .append(commit, branch.commit)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(protectedBranch)
                .append(commit)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("protectedBranch", protectedBranch)
                .append("commit", commit)
                .toString();
    }
}
