package com.dabsquared.gitlabjenkins.gitlab.api.model;


import net.karneim.pojobuilder.GeneratePojoBuilder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Awardable {
    private Integer id;
    private String name;
    private User user;
    private Integer awardableId;

    public Awardable() { /* default-constructor for Resteasy-based-api-proxies */ }

    public Awardable(Integer id, String name, User user, Integer awardable_id) {
        this.id = id;
        this.name = name;
        this.user = user;
        this.awardableId = awardable_id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getAwardableId() {
        return awardableId;
    }

    public void setAwardableId(Integer awardableId) {
        this.awardableId = awardableId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Awardable that = (Awardable) o;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(name, that.name)
                .append(user, that.user)
                .append(awardableId, that.awardableId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(user)
                .append(awardableId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("user", user)
                .append("awardableId", awardableId)
                .toString();
    }
}
