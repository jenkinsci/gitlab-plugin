package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.data.ObjectAttributes;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitStatus;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;

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
    		sourceProject = api.instance().getProject(objectAttributes.getSourceProjectId());
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

    public GitlabCommitStatus createCommitStatus(GitlabAPI api, String status, String targetUrl) {
        try {
            if (objectAttributes.getLastCommit() != null) {
                return api.createCommitStatus(sourceProject, objectAttributes.getLastCommit().getId(), status, objectAttributes.getLastCommit().getId(), "Jenkins", targetUrl, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
