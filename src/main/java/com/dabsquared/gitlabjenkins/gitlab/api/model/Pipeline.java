package com.dabsquared.gitlabjenkins.gitlab.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Pipeline {
    private Integer id;
    private String sha;
    private String status;
    private String source;

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

    /**
     * GitLab pipeline source, e.g. "merge_request_event", "push", "external", "trigger".
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
