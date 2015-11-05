package com.dabsquared.gitlabjenkins;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;

/**
 * Represents for WebHook payload
 *
 * @author Daniel Brooks
 */
public class GitLabMergeRequest extends GitLabRequest {

	public static GitLabMergeRequest create(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload should not be null");
        }
     
        GitLabMergeRequest pushRequest =  Builder.INSTANCE.get().fromJson(payload, GitLabMergeRequest.class);
        return pushRequest;
    }
    
    public GitLabMergeRequest() {
    }

    private String object_kind;

    private ObjectAttributes objectAttributes;
    private GitlabProject sourceProject = null;
    
    public GitlabProject getSourceProject (GitLab api) throws IOException {
    	if (sourceProject == null) {
    		sourceProject = api.instance().getProject(objectAttributes.sourceProjectId);
    	}
    	return sourceProject;
    }

    public String getObject_kind() {
        return object_kind;
    }

    public void setObject_kind(String objectKind) {
        this.object_kind = objectKind;
    }

    public ObjectAttributes getObjectAttribute() {
        return objectAttributes;
    }

    public void setObjectAttribute(ObjectAttributes objectAttributes) {
        this.objectAttributes = objectAttributes;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static class ObjectAttributes {

        private Integer id;

        private Integer iid;

        private String targetBranch;

        private String sourceBranch;

        private Integer sourceProjectId;

        private User author;

        private User assignee;

        private String title;

        private Date createdAt;

        private Date updatedAt;

        private String state;

        private String mergeStatus;

        private Integer targetProjectId;

        private String description;

        private Branch source;

        private Branch target;

        private LastCommit lastCommit;
        
        private String action;

        public ObjectAttributes() {
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getIid() {
            return iid;
        }

        public void setIid(Integer iid) {
            this.iid = iid;
        }

        public String getTargetBranch() {
            return targetBranch;
        }

        public void setTargetBranch(String targetBranch) {
            this.targetBranch = targetBranch;
        }

        public String getSourceBranch() {
            return sourceBranch;
        }

        public void setSourceBranch(String sourceBranch) {
            this.sourceBranch = sourceBranch;
        }

        public Integer getSourceProjectId() {
            return sourceProjectId;
        }

        public void setSourceProjectId(Integer sourceProjectId) {
            this.sourceProjectId = sourceProjectId;
        }

        public User getAuthor() {
            return author;
        }

        public void setAuthor(User author) {
            this.author = author;
        }

        public void setAuthor(GitlabUser author) {
            this.author = new User();
            this.author.setId(author.getId());
            this.author.setEmail(author.getEmail());
            this.author.setName(author.getName());
        }

        public User getAssignee() {
            return assignee;
        }

        public void setAssignee(User assignee) {
            this.assignee = assignee;
        }

        public void setAssignee(GitlabUser assignee) {
            this.assignee = new User();
            this.assignee.setId(assignee.getId());
            this.assignee.setEmail(assignee.getEmail());
            this.assignee.setName(assignee.getName());
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getMergeStatus() {
            return mergeStatus;
        }

        public void setMergeStatus(String mergeStatus) {
            this.mergeStatus = mergeStatus;
        }

        public Integer getTargetProjectId() {
            return targetProjectId;
        }

        public void setTargetProjectId(Integer targetProjectId) {
            this.targetProjectId = targetProjectId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Branch getSource() {
            return source;
        }

        public void setSource(Branch source) {
            this.source = source;
        }

        public Branch getTarget() {
            return target;
        }

        public void setTarget(Branch target) {
            this.target = target;
        }

        public LastCommit getLastCommit() {
            return lastCommit;
        }

        public void setLastCommit(LastCommit lastCommit) {
            this.lastCommit = lastCommit;
        }
        
        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }

    public static class Branch{
        private String name;
        private String ssh_url;
        private String http_url;
        private String namespace;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSsh_url() {
            return ssh_url;
        }

        public void setSsh_url(String ssh_url) {
            this.ssh_url = ssh_url;
        }

        public String getHttp_url() {
            return http_url;
        }

        public void setHttp_url(String http_url) {
            this.http_url = http_url;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }
    public static class LastCommit{
        private String id;
        private String message;
        private String url;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }


        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }
    public static class User {

        private Integer id;

        private String name;

        private String email;

        public User() {
        }

        public Integer getId() { return id;}

        public void setId(Integer id) { this.id = id; }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }
}
