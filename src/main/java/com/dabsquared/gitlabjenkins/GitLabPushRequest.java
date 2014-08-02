package com.dabsquared.gitlabjenkins;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.JavaIdentifierTransformer;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents for WebHook payload
 *
 * @author Daniel Brooks
 */
public class GitLabPushRequest {

    public static GitLabPushRequest create(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload should not be null");
        }
        return create(JSONObject.fromObject(payload));
    }

    public static GitLabPushRequest create(JSONObject payload) {
        if (payload == null || payload.isNullObject()) {
            throw new IllegalArgumentException("payload should not be null");
        }

        JsonConfig config = createJsonConfig();
        return (GitLabPushRequest) JSONObject.toBean(payload, config);
    }

    private static JsonConfig createJsonConfig() {
        JsonConfig config = new JsonConfig();
        config.setRootClass(GitLabPushRequest.class);

        Map<String, Class<?>> classMap = new HashMap<String, Class<?>>();
        classMap.put("commits", Commit.class);
        config.setClassMap(classMap);

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

    public GitLabPushRequest() {
    }


    private String before;

    private String after;

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
