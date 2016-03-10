package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.data.Commit;
import com.dabsquared.gitlabjenkins.data.Repository;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabProject;

import java.io.IOException;
import java.util.List;

/**
 * Represents for WebHook payload
 *
 * @author Daniel Brooks
 */
public class GitLabPushRequest extends GitLabRequest {
    public static GitLabPushRequest create(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload should not be null");
        }
     
        GitLabPushRequest pushRequest =  Builder.INSTANCE.get().fromJson(payload, GitLabPushRequest.class);
        return pushRequest;
    }

    public GitLabPushRequest() {
    }

    private GitlabProject sourceProject = null;

    public GitlabProject getSourceProject(GitlabAPI gitlabAPI) throws IOException {
        if (sourceProject == null) {
            sourceProject = gitlabAPI.getProject(project_id);
        }
        return sourceProject;
    }

    private String before;
    private String after;
    private String checkout_sha;
    private String ref;
    private Integer user_id;
    private String user_name;
    private Integer project_id;
    private Integer total_commits_count;
    private Repository repository;
    private List<Commit> commits;
    
    public List<Commit> getCommits() {
        return commits;
    }
    public Commit getLastCommit() {
        if (commits.isEmpty()) {
            return null;
        }
        return commits.get(commits.size() - 1);
    }
    
    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Integer getTotal_commits_count() {
        return total_commits_count;
    }

    public void setTotal_commits_count(Integer totalCommitsCount) {
        this.total_commits_count = totalCommitsCount;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer userId) {
        this.user_id = userId;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String userName) {
        this.user_name = userName;
    }

    public Integer getProject_id() {
        return project_id;
    }

    public void setProject_id(Integer projectId) {
        this.project_id = projectId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public String getCheckout_sha() {
        return checkout_sha;
    }

    public void setCheckout_sha(String checkout_sha) {
        this.checkout_sha = checkout_sha;
    }

}
