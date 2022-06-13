package com.dabsquared.gitlabjenkins.gitlab.api.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import net.karneim.pojobuilder.GeneratePojoBuilder;

/**
 * Project Hook 
 * @author <a href="mailto:jetune@kube-cloud.com">Jean-Jacques ETUNE NGI (Java EE Technical Lead / Enterprise Architect)</a>
 * @since Sun, 2022-06-12 - 12:25:26
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class ProjectHook {
	
	/**
	 * Hook ID
	 */
	private Integer id;
	
	/**
	 * Project ID
	 */
	private String projectId;
	
	/**
	 * Hook URL
	 */
	private String url;
	
	/**
	 * Push Event Triggering Status
	 */
	private Boolean pushEvents;

	/**
	 * Tag Push Event Triggering Status
	 */
	private Boolean tagPushEvents;

	/**
	 * Merge Request Event Triggering Status
	 */
	private Boolean mergeRequestsEvents;

	/**
	 * Repository Update Event Triggering Status
	 */
	private Boolean repositoryUpdateEvents;

	/**
	 * Issues Event Triggering Status
	 */
	private Boolean issuesEvents;

	/**
	 * Confidential Issue Event Triggering Status
	 */
	private Boolean confidentialIssuesEvents;

	/**
	 * Note Event Triggering Status
	 */
	private Boolean noteEvents;

	/**
	 * Confidential Note Event Triggering Status
	 */
	private Boolean confidentialNoteEvents;

	/**
	 * Pipeline Event Triggering Status
	 */
	private Boolean pipelineEvents;

	/**
	 * Wiki Page Event Triggering Status
	 */
	private Boolean wikiPageEvents;

	/**
	 * Deployment Event Triggering Status
	 */
	private Boolean deploymentEvents;

	/**
	 * Job Event Triggering Status
	 */
	private Boolean jobEvents;

	/**
	 * Releases Event Triggering Status
	 */
	private Boolean releasesEvents;

	/**
	 * Push Event Branch Filter
	 */
	private String pushEventsBranchFilter;

	/**
	 * Enable SSL Verification Status
	 */
	private Boolean enableSslVerification;
	
	/**
	 * Default Constructor
	 */
	public ProjectHook() {}
	
	public ProjectHook(Integer id, String projectId, String url, Boolean pushEvents, Boolean tagPushEvents,
			Boolean mergeRequestsEvents, Boolean repositoryUpdateEvents, Boolean issuesEvents,
			Boolean confidentialIssuesEvents, Boolean noteEvents, Boolean confidentialNoteEvents,
			Boolean pipelineEvents, Boolean wikiPageEvents, Boolean deploymentEvents, Boolean jobEvents,
			Boolean releasesEvents, String pushEventsBranchFilter, Boolean enableSslVerification) {
		this.id = id;
		this.projectId = projectId;
		this.url = url;
		this.pushEvents = pushEvents;
		this.tagPushEvents = tagPushEvents;
		this.mergeRequestsEvents = mergeRequestsEvents;
		this.repositoryUpdateEvents = repositoryUpdateEvents;
		this.issuesEvents = issuesEvents;
		this.confidentialIssuesEvents = confidentialIssuesEvents;
		this.noteEvents = noteEvents;
		this.confidentialNoteEvents = confidentialNoteEvents;
		this.pipelineEvents = pipelineEvents;
		this.wikiPageEvents = wikiPageEvents;
		this.deploymentEvents = deploymentEvents;
		this.jobEvents = jobEvents;
		this.releasesEvents = releasesEvents;
		this.pushEventsBranchFilter = pushEventsBranchFilter;
		this.enableSslVerification = enableSslVerification;
	}
	
	/**
	 * Method used to get the value of field "id"
	 * @return Value of field "id"
	 */
	public Integer getId() {
	
		// Return value
		return id;
	}

	/**
	 * Method used to update value of field  "id"
	 * @param id New value of field "id"
	 */
	public void setId(Integer id) {
	
		// Update value
		this.id = id;
	}

	/**
	 * Method used to get the value of field "projectId"
	 * @return Value of field "projectId"
	 */
	public String getProjectId() {
	
		// Return value
		return projectId;
	}

	/**
	 * Method used to update value of field  "projectId"
	 * @param projectId New value of field "projectId"
	 */
	public void setProjectId(String projectId) {
	
		// Update value
		this.projectId = projectId;
	}

	/**
	 * Method used to get the value of field "url"
	 * @return Value of field "url"
	 */
	public String getUrl() {
	
		// Return value
		return url;
	}

	/**
	 * Method used to update value of field  "url"
	 * @param url New value of field "url"
	 */
	public void setUrl(String url) {
	
		// Update value
		this.url = url;
	}

	/**
	 * Method used to get the value of field "pushEvents"
	 * @return Value of field "pushEvents"
	 */
	public Boolean getPushEvents() {
	
		// Return value
		return pushEvents;
	}

	/**
	 * Method used to update value of field  "pushEvents"
	 * @param pushEvents New value of field "pushEvents"
	 */
	public void setPushEvents(Boolean pushEvents) {
	
		// Update value
		this.pushEvents = pushEvents;
	}

	/**
	 * Method used to get the value of field "tagPushEvents"
	 * @return Value of field "tagPushEvents"
	 */
	public Boolean getTagPushEvents() {
	
		// Return value
		return tagPushEvents;
	}

	/**
	 * Method used to update value of field  "tagPushEvents"
	 * @param tagPushEvents New value of field "tagPushEvents"
	 */
	public void setTagPushEvents(Boolean tagPushEvents) {
	
		// Update value
		this.tagPushEvents = tagPushEvents;
	}

	/**
	 * Method used to get the value of field "mergeRequestsEvents"
	 * @return Value of field "mergeRequestsEvents"
	 */
	public Boolean getMergeRequestsEvents() {
	
		// Return value
		return mergeRequestsEvents;
	}

	/**
	 * Method used to update value of field  "mergeRequestsEvents"
	 * @param mergeRequestsEvents New value of field "mergeRequestsEvents"
	 */
	public void setMergeRequestsEvents(Boolean mergeRequestsEvents) {
	
		// Update value
		this.mergeRequestsEvents = mergeRequestsEvents;
	}

	/**
	 * Method used to get the value of field "repositoryUpdateEvents"
	 * @return Value of field "repositoryUpdateEvents"
	 */
	public Boolean getRepositoryUpdateEvents() {
	
		// Return value
		return repositoryUpdateEvents;
	}

	/**
	 * Method used to update value of field  "repositoryUpdateEvents"
	 * @param repositoryUpdateEvents New value of field "repositoryUpdateEvents"
	 */
	public void setRepositoryUpdateEvents(Boolean repositoryUpdateEvents) {
	
		// Update value
		this.repositoryUpdateEvents = repositoryUpdateEvents;
	}

	/**
	 * Method used to get the value of field "issuesEvents"
	 * @return Value of field "issuesEvents"
	 */
	public Boolean getIssuesEvents() {
	
		// Return value
		return issuesEvents;
	}

	/**
	 * Method used to update value of field  "issuesEvents"
	 * @param issuesEvents New value of field "issuesEvents"
	 */
	public void setIssuesEvents(Boolean issuesEvents) {
	
		// Update value
		this.issuesEvents = issuesEvents;
	}

	/**
	 * Method used to get the value of field "confidentialIssuesEvents"
	 * @return Value of field "confidentialIssuesEvents"
	 */
	public Boolean getConfidentialIssuesEvents() {
	
		// Return value
		return confidentialIssuesEvents;
	}

	/**
	 * Method used to update value of field  "confidentialIssuesEvents"
	 * @param confidentialIssuesEvents New value of field "confidentialIssuesEvents"
	 */
	public void setConfidentialIssuesEvents(Boolean confidentialIssuesEvents) {
	
		// Update value
		this.confidentialIssuesEvents = confidentialIssuesEvents;
	}

	/**
	 * Method used to get the value of field "noteEvents"
	 * @return Value of field "noteEvents"
	 */
	public Boolean getNoteEvents() {
	
		// Return value
		return noteEvents;
	}

	/**
	 * Method used to update value of field  "noteEvents"
	 * @param noteEvents New value of field "noteEvents"
	 */
	public void setNoteEvents(Boolean noteEvents) {
	
		// Update value
		this.noteEvents = noteEvents;
	}

	/**
	 * Method used to get the value of field "confidentialNoteEvents"
	 * @return Value of field "confidentialNoteEvents"
	 */
	public Boolean getConfidentialNoteEvents() {
	
		// Return value
		return confidentialNoteEvents;
	}

	/**
	 * Method used to update value of field  "confidentialNoteEvents"
	 * @param confidentialNoteEvents New value of field "confidentialNoteEvents"
	 */
	public void setConfidentialNoteEvents(Boolean confidentialNoteEvents) {
	
		// Update value
		this.confidentialNoteEvents = confidentialNoteEvents;
	}

	/**
	 * Method used to get the value of field "pipelineEvents"
	 * @return Value of field "pipelineEvents"
	 */
	public Boolean getPipelineEvents() {
	
		// Return value
		return pipelineEvents;
	}

	/**
	 * Method used to update value of field  "pipelineEvents"
	 * @param pipelineEvents New value of field "pipelineEvents"
	 */
	public void setPipelineEvents(Boolean pipelineEvents) {
	
		// Update value
		this.pipelineEvents = pipelineEvents;
	}

	/**
	 * Method used to get the value of field "wikiPageEvents"
	 * @return Value of field "wikiPageEvents"
	 */
	public Boolean getWikiPageEvents() {
	
		// Return value
		return wikiPageEvents;
	}

	/**
	 * Method used to update value of field  "wikiPageEvents"
	 * @param wikiPageEvents New value of field "wikiPageEvents"
	 */
	public void setWikiPageEvents(Boolean wikiPageEvents) {
	
		// Update value
		this.wikiPageEvents = wikiPageEvents;
	}

	/**
	 * Method used to get the value of field "deploymentEvents"
	 * @return Value of field "deploymentEvents"
	 */
	public Boolean getDeploymentEvents() {
	
		// Return value
		return deploymentEvents;
	}

	/**
	 * Method used to update value of field  "deploymentEvents"
	 * @param deploymentEvents New value of field "deploymentEvents"
	 */
	public void setDeploymentEvents(Boolean deploymentEvents) {
	
		// Update value
		this.deploymentEvents = deploymentEvents;
	}

	/**
	 * Method used to get the value of field "jobEvents"
	 * @return Value of field "jobEvents"
	 */
	public Boolean getJobEvents() {
	
		// Return value
		return jobEvents;
	}

	/**
	 * Method used to update value of field  "jobEvents"
	 * @param jobEvents New value of field "jobEvents"
	 */
	public void setJobEvents(Boolean jobEvents) {
	
		// Update value
		this.jobEvents = jobEvents;
	}

	/**
	 * Method used to get the value of field "releasesEvents"
	 * @return Value of field "releasesEvents"
	 */
	public Boolean getReleasesEvents() {
	
		// Return value
		return releasesEvents;
	}

	/**
	 * Method used to update value of field  "releasesEvents"
	 * @param releasesEvents New value of field "releasesEvents"
	 */
	public void setReleasesEvents(Boolean releasesEvents) {
	
		// Update value
		this.releasesEvents = releasesEvents;
	}

	/**
	 * Method used to get the value of field "pushEventsBranchFilter"
	 * @return Value of field "pushEventsBranchFilter"
	 */
	public String getPushEventsBranchFilter() {
	
		// Return value
		return pushEventsBranchFilter;
	}

	/**
	 * Method used to update value of field  "pushEventsBranchFilter"
	 * @param pushEventsBranchFilter New value of field "pushEventsBranchFilter"
	 */
	public void setPushEventsBranchFilter(String pushEventsBranchFilter) {
	
		// Update value
		this.pushEventsBranchFilter = pushEventsBranchFilter;
	}

	/**
	 * Method used to get the value of field "enableSslVerification"
	 * @return Value of field "enableSslVerification"
	 */
	public Boolean getEnableSslVerification() {
	
		// Return value
		return enableSslVerification;
	}

	/**
	 * Method used to update value of field  "enableSslVerification"
	 * @param enableSslVerification New value of field "enableSslVerification"
	 */
	public void setEnableSslVerification(Boolean enableSslVerification) {
	
		// Update value
		this.enableSslVerification = enableSslVerification;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProjectHook project = (ProjectHook) o;
		return new EqualsBuilder()
				.append(id, project.id)
				.isEquals();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(id)
				.toHashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("id", id)
				.append("name", projectId)
				.append("callBack", url)
				.append("PushEvent", pushEvents)
				.append("mergeRequestsEvents", mergeRequestsEvents)
				.append("noteEvents", noteEvents)
				.append("tagPushEvents", tagPushEvents)
				.append("repositoryUpdateEvents", repositoryUpdateEvents)
				.append("jobEvents", jobEvents)
				.append("pipelineEvents", pipelineEvents)
				.toString();
	}
}
