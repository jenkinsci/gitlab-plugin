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
public class User {

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
    
    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public User(String name, String username, String email, String avatarUrl) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    User() {
        this(null, null, null, null);
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
                .append(name, user.name)
                .append(username, user.username)
                .append(email, user.email)
                .append(avatarUrl, user.avatarUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(username)
                .append(email)
                .append(avatarUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("username", username)
                .append("email", email)
                .append("avatarUrl", avatarUrl)
                .toString();
    }
}
