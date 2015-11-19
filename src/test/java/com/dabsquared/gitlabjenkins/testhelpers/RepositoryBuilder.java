package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.GitLabPushRequest;

public class RepositoryBuilder {

	public static GitLabPushRequest.Repository buildWithDefaults() {
		return new RepositoryBuilder().withBasicValues().build();
	}

	private GitLabPushRequest.Repository repository;

	public RepositoryBuilder() {
		repository = new GitLabPushRequest.Repository();
	}

	public RepositoryBuilder withBasicValues() {
		repository.setName("test-repo");
		repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
		repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
		repository.setDescription("");
		return this;
	}

	public GitLabPushRequest.Repository build() {
		return repository;
	}

}
