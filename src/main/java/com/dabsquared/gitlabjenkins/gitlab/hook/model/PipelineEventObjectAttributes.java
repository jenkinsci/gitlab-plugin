package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import java.util.Date;
import java.util.List;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Milena Zachow
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class PipelineEventObjectAttributes {

    private Integer id;
    private Integer iid;
    private String ref;
    private boolean tag;
    private String sha;
    private String beforeSha;
    private String source;
    private String status;
    private List<String> stages;
    private Date createdAt;
    private Date finishedAt;
    private int duration;
    private String url;

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

    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
                .append(iid, that.iid)
                .append(ref, that.ref)
                .append(tag, that.tag)
                .append(sha, that.sha)
                .append(beforeSha, that.beforeSha)
                .append(source, that.source)
                .append(status, that.status)
                .append(stages, that.stages)
                .append(createdAt, that.createdAt)
                .append(finishedAt, that.finishedAt)
                .append(duration, that.duration)
                .append(url, that.url)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(iid)
                .append(ref)
                .append(tag)
                .append(sha)
                .append(beforeSha)
                .append(source)
                .append(status)
                .append(stages)
                .append(createdAt)
                .append(finishedAt)
                .append(duration)
                .append(url)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("iid", iid)
                .append("ref", ref)
                .append("tag", tag)
                .append("sha", sha)
                .append("beforeSha", beforeSha)
                .append("source", source)
                .append("status", status)
                .append("stages", stages)
                .append("createdAt", createdAt)
                .append("finishedAt", finishedAt)
                .append("duration", duration)
                .append("url", url)
                .toString();
    }
}
