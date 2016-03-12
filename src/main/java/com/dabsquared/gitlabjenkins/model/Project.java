package com.dabsquared.gitlabjenkins.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public class Project {

    private final String name;
    private final String description;
    private final String webUrl;
    private final String avatarUrl;
    private final String namespace;
    private final Integer visibilityLevel;
    private final String pathWithNamespace;
    private final String defaultBranch;
    private final String homepage;
    private final String url;
    private final String sshUrl;
    private final String httpUrl;

    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public Project(String name, String description, String webUrl, String avatarUrl, String namespace, Integer visibilityLevel,
                   String pathWithNamespace, String defaultBranch, String homepage, String url, String sshUrl, String httpUrl) {
        this.name = name;
        this.description = description;
        this.webUrl = webUrl;
        this.avatarUrl = avatarUrl;
        this.namespace = namespace;
        this.visibilityLevel = visibilityLevel;
        this.pathWithNamespace = pathWithNamespace;
        this.defaultBranch = defaultBranch;
        this.homepage = homepage;
        this.url = url;
        this.sshUrl = sshUrl;
        this.httpUrl = httpUrl;
    }

    Project() {
        this(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public Integer getVisibilityLevel() {
        return visibilityLevel;
    }

    public String getPathWithNamespace() {
        return pathWithNamespace;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getHomepage() {
        return homepage;
    }

    public String getUrl() {
        return url;
    }

    public String getSshUrl() {
        return sshUrl;
    }

    public String getHttpUrl() {
        return httpUrl;
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
