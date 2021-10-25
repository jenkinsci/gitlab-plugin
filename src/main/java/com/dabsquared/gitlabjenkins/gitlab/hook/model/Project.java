package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Project {

    private Integer id;
    private String name;
    private String description;
    private String webUrl;
    private String avatarUrl;
    private String namespace;
    private Integer visibilityLevel;
    private String pathWithNamespace;
    private String defaultBranch;
    private String homepage;
    private String url;
    private String sshUrl;
    private String httpUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Integer getVisibilityLevel() {
        return visibilityLevel;
    }

    public void setVisibilityLevel(Integer visibilityLevel) {
        this.visibilityLevel = visibilityLevel;
    }

    public String getPathWithNamespace() {
        return pathWithNamespace;
    }

    public void setPathWithNamespace(String pathWithNamespace) {
        this.pathWithNamespace = pathWithNamespace;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Project project = (Project) o;
        return new EqualsBuilder()
                .append(id, project.id)
                .append(name, project.name)
                .append(description, project.description)
                .append(webUrl, project.webUrl)
                .append(avatarUrl, project.avatarUrl)
                .append(namespace, project.namespace)
                .append(visibilityLevel, project.visibilityLevel)
                .append(pathWithNamespace, project.pathWithNamespace)
                .append(defaultBranch, project.defaultBranch)
                .append(homepage, project.homepage)
                .append(url, project.url)
                .append(sshUrl, project.sshUrl)
                .append(httpUrl, project.httpUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(description)
                .append(webUrl)
                .append(avatarUrl)
                .append(namespace)
                .append(visibilityLevel)
                .append(pathWithNamespace)
                .append(defaultBranch)
                .append(homepage)
                .append(url)
                .append(sshUrl)
                .append(httpUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("description", description)
                .append("webUrl", webUrl)
                .append("avatarUrl", avatarUrl)
                .append("namespace", namespace)
                .append("visibilityLevel", visibilityLevel)
                .append("pathWithNamespace", pathWithNamespace)
                .append("defaultBranch", defaultBranch)
                .append("homepage", homepage)
                .append("url", url)
                .append("sshUrl", sshUrl)
                .append("httpUrl", httpUrl)
                .toString();
    }
}
