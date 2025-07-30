package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import java.util.Date;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Benjamin ROBIN
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestLabel {

    /*
        "id": 206,
        "title": "API",
        "color": "#ffffff",
        "project_id": 14,
        "created_at": "2013-12-03T17:15:43Z",
        "updated_at": "2013-12-03T17:15:43Z",
        "template": false,
        "description": "API related issues",
        "type": "ProjectLabel",
        "group_id": 41
    */
    private Integer id;
    private String title;
    private String color;
    private Integer projectId;
    private Date createdAt;
    private Date updatedAt;
    private Boolean template;
    private String description;
    private String type;
    private Integer groupId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getTemplate() {
        return template;
    }

    public void setTemplate(Boolean template) {
        this.template = template;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestLabel that = (MergeRequestLabel) o;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(title, that.title)
                .append(color, that.color)
                .append(projectId, that.projectId)
                .append(createdAt, that.createdAt)
                .append(updatedAt, that.updatedAt)
                .append(template, that.template)
                .append(description, that.description)
                .append(type, that.type)
                .append(groupId, that.groupId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(title)
                .append(color)
                .append(projectId)
                .append(createdAt)
                .append(updatedAt)
                .append(template)
                .append(description)
                .append(type)
                .append(groupId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("title", title)
                .append("color", color)
                .append("projectId", projectId)
                .append("createdAt", createdAt)
                .append("updatedAt", updatedAt)
                .append("template", template)
                .append("description", description)
                .append("type", type)
                .append("groupId", groupId)
                .toString();
    }
}
