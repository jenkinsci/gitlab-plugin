package com.dabsquared.gitlabjenkins.models.request;

import com.dabsquared.gitlabjenkins.GitLab;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.models.ObjectAttributes;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitStatus;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents for WebHook payload
 *
 * @author Daniel Brooks
 */
public class GitLabMergeRequest extends GitLabRequest {
    private static Logger LOG = Logger.getLogger(GitLabMergeRequest.class.getName());

    private String object_kind;
    private ObjectAttributes objectAttributes;
    private GitlabProject sourceProject = null;

    public GitLabMergeRequest() {
    }

    public static GitLabMergeRequest create(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload should not be null");
        }

        return Builder.INSTANCE.get().fromJson(payload, GitLabMergeRequest.class);
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
                GitlabProject sourceProject = getSourceProject(GitLabPushTrigger.getDesc().getGitlab());
                return api.createCommitStatus(sourceProject, objectAttributes.getLastCommit().getId(), status, objectAttributes.getLastCommit().getId(), "Jenkins", targetUrl, null);
            }
        } catch (IOException e) {
            LOG.severe("Failed to create commit status:");
            LOG.severe(e.toString());
        }

        return null;
    }

    public GitlabProject getSourceProject(GitLab api) throws IOException {
        if (sourceProject == null) {
            sourceProject = api.instance().getProject(objectAttributes.getSourceProjectId());
        }
        return sourceProject;
    }
}
