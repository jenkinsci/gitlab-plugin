package com.dabsquared.gitlabjenkins.gitlab.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public class User {

    private final Integer id;
    private final String name;
    private final String username;
    private final String email;
    private final String avatarUrl;

    public static Supplier<User> nullUser() {
        return new Supplier<User>() {
            @Override
            public User get() {
                return new User();
            }
        };
    }

    @JsonCreator
    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public User(@JsonProperty("id") Integer id,
                @JsonProperty("name") String name,
                @JsonProperty("username") String username,
                @JsonProperty("email") String email,
                @JsonProperty("avatar_url") String avatarUrl) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    User() {
        this(null, null, null, null, null);
    }

    public Optional<Integer> optId() {
        return Optional.fromNullable(id);
    }

    public Optional<String> optName() {
        return Optional.fromNullable(name);
    }

    public Optional<String> optUsername() {
        return Optional.fromNullable(username);
    }

    public Optional<String> optEmail() {
        return Optional.fromNullable(email);
    }

    public Optional<String> optAvatarUrl() {
        return Optional.fromNullable(avatarUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return new EqualsBuilder()
                .append(id, user.id)
                .append(name, user.name)
                .append(username, user.username)
                .append(email, user.email)
                .append(avatarUrl, user.avatarUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(username)
                .append(email)
                .append(avatarUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("username", username)
                .append("email", email)
                .append("avatarUrl", avatarUrl)
                .toString();
    }
}
