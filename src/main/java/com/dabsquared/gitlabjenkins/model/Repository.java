package com.dabsquared.gitlabjenkins.model;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public class Repository {

    private final String name;
    private final String description;
    private final String url;
    private final String homepage;
    private final String gitSshUrl;
    private final String gitHttpUrl;
    private final Integer visibilityLevel;

    public static Supplier<Repository> nullRepository() {
        return new Supplier<Repository>() {
            @Override
            public Repository get() {
                return new Repository();
            }
        };
    }
    
    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public Repository(String name, String description, String url, String homepage, String gitSshUrl, String gitHttpUrl, Integer visibilityLevel) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.homepage = homepage;
        this.gitSshUrl = gitSshUrl;
        this.gitHttpUrl = gitHttpUrl;
        this.visibilityLevel = visibilityLevel;
    }

    Repository() {
        this(null, null, null, null, null, null, null);
    }

    public Optional<String> optName() {
        return Optional.fromNullable(name);
    }

    public Optional<String> optDescription() {
        return Optional.fromNullable(description);
    }

    public Optional<String> optUrl() {
        return Optional.fromNullable(url);
    }

    public Optional<String> optHomepage() {
        return Optional.fromNullable(homepage);
    }

    public Optional<String> optGitSshUrl() {
        return Optional.fromNullable(gitSshUrl);
    }

    public Optional<String> optGitHttpUrl() {
        return Optional.fromNullable(gitHttpUrl);
    }

    public Optional<Integer> optVisibilityLevel() {
        return Optional.fromNullable(visibilityLevel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Repository that = (Repository) o;
        return new EqualsBuilder()
                .append(name, that.name)
                .append(description, that.description)
                .append(url, that.url)
                .append(homepage, that.homepage)
                .append(gitSshUrl, that.gitSshUrl)
                .append(gitHttpUrl, that.gitHttpUrl)
                .append(visibilityLevel, that.visibilityLevel)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(description)
                .append(url)
                .append(homepage)
                .append(gitSshUrl)
                .append(gitHttpUrl)
                .append(visibilityLevel)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("description", description)
                .append("url", url)
                .append("homepage", homepage)
                .append("gitSshUrl", gitSshUrl)
                .append("gitHttpUrl", gitHttpUrl)
                .append("visibilityLevel", visibilityLevel)
                .toString();
    }
}
