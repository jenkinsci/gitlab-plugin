package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@SuppressWarnings({"WeakerAccess", "unused"})
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SystemHook extends WebHook {
    private static final List<String> CREATE_EVENTS = singletonList("project_create");
    private static final List<String> REMOVE_EVENTS = singletonList("project_destroy");
    private static final List<String> UPDATE_EVENTS = asList("user_add_to_team", "user_remove_from_team");

    private Date created;
    private Date updated;
    private String eventName;
    private String projectName;
    private String ownerName;
    private String ownerEmail;
    private String path;
    private String pathWithNamespace;
    private Integer projectId;
    private String visibility;
    private String userUsername;

    public SystemHook() {
    }

    public Date getCreatedAt() {
        return this.created;
    }

    public void setCreatedAt(Date created) {
        this.created = created;
    }

    public Date getUpdatedAt() {
        return this.updated;
    }

    public void setUpdatedAt(Date updated) {
        this.updated = updated;
    }

    public String getEventName() {
        return this.eventName;
    }

    public void setEventName(String name) {
        this.eventName = name;
    }

    public String getName() {
        return this.projectName;
    }

    public void setName(String name) {
        this.projectName = name;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return this.ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getPathWithNamespace() {
        return this.pathWithNamespace;
    }

    public void setPathWithNamespace(String path) {
        this.pathWithNamespace = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getProjectVisibility() {
        return visibility;
    }

    public void setProjectVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUserName(String userUsername) {
        this.userUsername = userUsername;
    }

    @Override
    public String getObjectKind() {
        return super.getObjectKind() != null ? super.getObjectKind() : getEventName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SystemHook that = (SystemHook) o;
        return Objects.equals(getCreatedAt(), that.getCreatedAt()) &&
                Objects.equals(getUpdatedAt(), that.getUpdatedAt()) &&
                Objects.equals(getEventName(), that.getEventName()) &&
                Objects.equals(getName(), that.getName()) &&
                Objects.equals(getOwnerName(), that.getOwnerName()) &&
                Objects.equals(getOwnerEmail(), that.getOwnerEmail()) &&
                Objects.equals(getPath(), that.getPath()) &&
                Objects.equals(getPathWithNamespace(), that.getPathWithNamespace()) &&
                Objects.equals(getProjectId(), that.getProjectId()) &&
                Objects.equals(getProjectVisibility(), that.getProjectVisibility());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getCreatedAt(), getUpdatedAt(), getEventName(), getName(), getOwnerName(), getOwnerEmail(), getPath(), getPathWithNamespace(), getProjectId(), getProjectVisibility());
    }

    @Override
    public String toString() {
        return "SystemHook{" +
                "created=" + created +
                ", updated=" + updated +
                ", eventName='" + eventName + '\'' +
                ", projectName='" + projectName + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", path='" + path + '\'' +
                ", pathWithNamespace='" + pathWithNamespace + '\'' +
                ", projectId=" + projectId +
                ", visibility='" + visibility + '\'' +
                '}';
    }

    public boolean isProjectCreated() {
        return CREATE_EVENTS.contains(getEventName());
    }

    public boolean isProjectDestroyed() {
        return REMOVE_EVENTS.contains(getEventName()) ;
    }

    public boolean isProjectUpdated() {
        return UPDATE_EVENTS.contains(getEventName());
    }
}
