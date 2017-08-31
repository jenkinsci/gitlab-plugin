package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;
import java.util.List;

/**
 * @author Milena Zachow
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class PipelineEventObjectAttributes {

    private Integer id;
    private String ref;
    private boolean tag;
    private String sha;
    private String beforeSha;
    private String status;
    private List<String> stages;
    private Date createdAt;
    private Date finishedAt;
    private int duration;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public boolean getIsTag() {
        return tag;
    }

    public void setTag(boolean tag) {
        this.tag = tag;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getBeforeSha() {
        return beforeSha;
    }

    public void setBeforeSha(String beforeSha) {
        this.beforeSha = beforeSha;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getStages() {
        return stages;
    }

    public void setStages(List<String> stages) {
        this.stages = stages;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }




    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineEventObjectAttributes that = (PipelineEventObjectAttributes) o;
        return new EqualsBuilder()
            .append(id, that.id)
            .append(ref, that.ref)
            .append(tag, that.tag)
            .append(sha, that.sha)
            .append(beforeSha, that.beforeSha)
            .append(status, that.status)
            .append(stages, that.stages)
            .append(createdAt, that.createdAt)
            .append(finishedAt, that.finishedAt)
            .append(duration, that.duration)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(ref)
            .append(tag)
            .append(sha)
            .append(beforeSha)
            .append(status)
            .append(stages)
            .append(createdAt)
            .append(finishedAt)
            .append(duration)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("ref", ref)
            .append("tag", tag)
            .append("sha", sha)
            .append("beforeSha", beforeSha)
            .append("status", status)
            .append("stages", stages)
            .append("createdAt", createdAt)
            .append("finishedAt", finishedAt)
            .append("duration", duration)
            .toString();
    }
}
