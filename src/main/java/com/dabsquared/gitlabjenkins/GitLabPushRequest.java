package com.dabsquared.gitlabjenkins;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommitStatus;
import org.gitlab.api.models.GitlabProject;

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

    public GitlabProject getSourceProject (GitLab api) throws IOException {
        if (sourceProject == null) {
            sourceProject = api.instance().getProject(project_id);
        }
        return sourceProject;
    }

    public GitlabCommitStatus createCommitStatus(GitlabAPI api, String status, String targetUrl) {
        try {
            if(getLastCommit()!=null) {
                return api.createCommitStatus(sourceProject, getLastCommit().getId(), status, checkout_sha, "Jenkins", targetUrl, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    public static class Repository {

        private String name;

        private String url;

        private String description;

        private String homepage;

        public Repository() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getHomepage() {
            return homepage;
        }

        public void setHomepage(String homepage) {
            this.homepage = homepage;
        }


        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }

    }

    public static class Commit {

        private String id;

        private String message;

        private String timestamp;

        private String url;

        private User author;

        public Commit() {
        }

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

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public User getAuthor() {
            return author;
        }

        public void setAuthor(User author) {
            this.author = author;
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }

    }

    public static class User {

        private String name;

        private String email;

        public User() {
        }

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
