package com.dabsquared.gitlabjenkins.gitlab.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Gitlab Group
 *
 * @author <a href="mailto:jetune@kube-cloud.com">Jean-Jacques ETUNE NGI (Java EE Technical Lead /
 *     Enterprise Architect)</a>
 * @since Mon, 2022-06-13 - 07:19:01
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Group {

    /** Group Hook ID */
    private Integer id;

    /** Group Parent ID */
    private Integer parentId;

    /** Group Name */
    private String name;

    /** Group Full Name */
    private String fullName;

    /** Group Relative Path */
    private String path;

    /** Group Full Path */
    private String fullPath;

    /** Group Description */
    private String description;

    /** Group Visibulity */
    private String visibility;

    /** Group Web URL */
    private String webUrl;

    /** Group Avatar URL */
    private String avatarUrl;

    /** Group Default Branch Protection */
    private Integer defaultBranchProtection;

    /** Group Request Access Enabled */
    private Boolean requestAccessEnabled;

    /** Group Marked For Deletetion On Specifi Date (as String) */
    private String markedForDeletionOn;

    /**
     * Method used to get the value of field "id"
     *
     * @return Value of field "id"
     */
    public Integer getId() {

        // Return value
        return id;
    }

    /**
     * Method used to update value of field "id"
     *
     * @param id New value of field "id"
     */
    public void setId(Integer id) {

        // Update value
        this.id = id;
    }

    /**
     * Method used to get the value of field "parentId"
     *
     * @return Value of field "parentId"
     */
    public Integer getParentId() {

        // Return value
        return parentId;
    }

    /**
     * Method used to update value of field "parentId"
     *
     * @param parentId New value of field "parentId"
     */
    public void setParentId(Integer parentId) {

        // Update value
        this.parentId = parentId;
    }

    /**
     * Method used to get the value of field "name"
     *
     * @return Value of field "name"
     */
    public String getName() {

        // Return value
        return name;
    }

    /**
     * Method used to update value of field "name"
     *
     * @param name New value of field "name"
     */
    public void setName(String name) {

        // Update value
        this.name = name;
    }

    /**
     * Method used to get the value of field "fullName"
     *
     * @return Value of field "fullName"
     */
    public String getFullName() {

        // Return value
        return fullName;
    }

    /**
     * Method used to update value of field "fullName"
     *
     * @param fullName New value of field "fullName"
     */
    public void setFullName(String fullName) {

        // Update value
        this.fullName = fullName;
    }

    /**
     * Method used to get the value of field "path"
     *
     * @return Value of field "path"
     */
    public String getPath() {

        // Return value
        return path;
    }

    /**
     * Method used to update value of field "path"
     *
     * @param path New value of field "path"
     */
    public void setPath(String path) {

        // Update value
        this.path = path;
    }

    /**
     * Method used to get the value of field "fullPath"
     *
     * @return Value of field "fullPath"
     */
    public String getFullPath() {

        // Return value
        return fullPath;
    }

    /**
     * Method used to update value of field "fullPath"
     *
     * @param fullPath New value of field "fullPath"
     */
    public void setFullPath(String fullPath) {

        // Update value
        this.fullPath = fullPath;
    }

    /**
     * Method used to get the value of field "description"
     *
     * @return Value of field "description"
     */
    public String getDescription() {

        // Return value
        return description;
    }

    /**
     * Method used to update value of field "description"
     *
     * @param description New value of field "description"
     */
    public void setDescription(String description) {

        // Update value
        this.description = description;
    }

    /**
     * Method used to get the value of field "visibility"
     *
     * @return Value of field "visibility"
     */
    public String getVisibility() {

        // Return value
        return visibility;
    }

    /**
     * Method used to update value of field "visibility"
     *
     * @param visibility New value of field "visibility"
     */
    public void setVisibility(String visibility) {

        // Update value
        this.visibility = visibility;
    }

    /**
     * Method used to get the value of field "webUrl"
     *
     * @return Value of field "webUrl"
     */
    public String getWebUrl() {

        // Return value
        return webUrl;
    }

    /**
     * Method used to update value of field "webUrl"
     *
     * @param webUrl New value of field "webUrl"
     */
    public void setWebUrl(String webUrl) {

        // Update value
        this.webUrl = webUrl;
    }

    /**
     * Method used to get the value of field "avatarUrl"
     *
     * @return Value of field "avatarUrl"
     */
    public String getAvatarUrl() {

        // Return value
        return avatarUrl;
    }

    /**
     * Method used to update value of field "avatarUrl"
     *
     * @param avatarUrl New value of field "avatarUrl"
     */
    public void setAvatarUrl(String avatarUrl) {

        // Update value
        this.avatarUrl = avatarUrl;
    }

    /**
     * Method used to get the value of field "defaultBranchProtection"
     *
     * @return Value of field "defaultBranchProtection"
     */
    public Integer getDefaultBranchProtection() {

        // Return value
        return defaultBranchProtection;
    }

    /**
     * Method used to update value of field "defaultBranchProtection"
     *
     * @param defaultBranchProtection New value of field "defaultBranchProtection"
     */
    public void setDefaultBranchProtection(Integer defaultBranchProtection) {

        // Update value
        this.defaultBranchProtection = defaultBranchProtection;
    }

    /**
     * Method used to get the value of field "requestAccessEnabled"
     *
     * @return Value of field "requestAccessEnabled"
     */
    public Boolean getRequestAccessEnabled() {

        // Return value
        return requestAccessEnabled;
    }

    /**
     * Method used to update value of field "requestAccessEnabled"
     *
     * @param requestAccessEnabled New value of field "requestAccessEnabled"
     */
    public void setRequestAccessEnabled(Boolean requestAccessEnabled) {

        // Update value
        this.requestAccessEnabled = requestAccessEnabled;
    }

    /**
     * Method used to get the value of field "markedForDeletionOn"
     *
     * @return Value of field "markedForDeletionOn"
     */
    public String getMarkedForDeletionOn() {

        // Return value
        return markedForDeletionOn;
    }

    /**
     * Method used to update value of field "markedForDeletionOn"
     *
     * @param markedForDeletionOn New value of field "markedForDeletionOn"
     */
    public void setMarkedForDeletionOn(String markedForDeletionOn) {

        // Update value
        this.markedForDeletionOn = markedForDeletionOn;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object parameter) {

        // If Objects references are the same
        if (this == parameter) {
            return true;
        }

        // If Given Parameter is Null or class don't match with actual object instance
        if (parameter == null || getClass() != parameter.getClass()) {
            return false;
        }

        // Cast to Target Class
        Group casted = (Group) parameter;

        // Return ID Comparison
        return new EqualsBuilder().append(id, casted.id).isEquals();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        // Return ID Hashcode
        return new HashCodeBuilder(17, 37).append(id).toHashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        // Build and Return Fields Values on Builder
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("fullName", fullName)
                .append("path", path)
                .append("fullPath", fullPath)
                .append("description", description)
                .append("visibility", visibility)
                .append("webUrl", webUrl)
                .append("avatarUrl", avatarUrl)
                .append("defaultBranchProtection", defaultBranchProtection)
                .append("requestAccessEnabled", requestAccessEnabled)
                .append("markedForDeletionOn", markedForDeletionOn)
                .toString();
    }
}
