package com.dabsquared.gitlabjenkins.gitlab.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Pipeline {
    private Integer id;
    private String sha;
    private String status;
    private String ref;
    private String source;
    private String createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * GitLab pipeline source, e.g. "merge_request_event", "push", "external", "trigger".
     * Used to prefer the MR pipeline when several pipelines exist for the same commit.
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * ISO-8601 timestamp string. Kept as a string (rather than parsed) since it's only
     * ever used for lexicographic sorting, which works correctly for ISO-8601.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
