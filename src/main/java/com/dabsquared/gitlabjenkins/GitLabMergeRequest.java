package com.dabsquared.gitlabjenkins;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.JavaIdentifierTransformer;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents for WebHook payload
 *
 * @author Daniel Brooks
 */
public class GitLabMergeRequest {

    public static GitLabMergeRequest create(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload should not be null");
        }
        return create(JSONObject.fromObject(payload));
    }

    public static GitLabMergeRequest create(JSONObject payload) {
        if (payload == null || payload.isNullObject()) {
            throw new IllegalArgumentException("payload should not be null");
        }

        JsonConfig config = createJsonConfig();
        return (GitLabMergeRequest) JSONObject.toBean(payload, config);
    }

    private static JsonConfig createJsonConfig() {
        JsonConfig config = new JsonConfig();
        config.setRootClass(GitLabMergeRequest.class);

        config.setJavaIdentifierTransformer(new JavaIdentifierTransformer() {

            @Override
            public String transformToJavaIdentifier(String param) {
                if (param == null) {
                    return null;
                }
                if ("private".equals(param)) {
                    return "private_";
                }
                return param;
            }

        });

        return config;
    }

    public GitLabMergeRequest() {
    }


    private String objectKind;

    private ObjectAttributes objectAttribute;

    public String getObjectKind() {
        return objectKind;
    }

    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public ObjectAttributes getObjectAttribute() {
        return objectAttribute;
    }

    public void setObjectAttribute(ObjectAttributes objectAttribute) {
        this.objectAttribute = objectAttribute;
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static class ObjectAttributes {

        private Integer id;

        private String targetBranch;

        private String sourceBranch;

        private Integer sourceProjectId;

        private Integer authorId;

        private Integer assigneeId;

        private String title;

        private Date createdAt;

        private Date updatedAt;

        private String state;

        private String mergeStatus;

        private Integer targetProjectId;

        private String description;


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

        public Integer getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Integer authorId) {
            this.authorId = authorId;
        }

        public Integer getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(Integer assigneeId) {
            this.assigneeId = assigneeId;
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
    }

}
