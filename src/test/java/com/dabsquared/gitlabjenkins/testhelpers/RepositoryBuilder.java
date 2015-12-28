package com.dabsquared.gitlabjenkins.testhelpers;


import com.dabsquared.gitlabjenkins.models.Repository;

public class RepositoryBuilder {

	public static Repository buildWithDefaults() {
		return new RepositoryBuilder().withBasicValues().build();
	}

	private Repository repository;

	public RepositoryBuilder() {
		repository = new Repository();
	}

	public RepositoryBuilder withBasicValues() {
		repository.setName("test-repo");
		repository.setUrl("git@gitlabserver.example.com:test-group/test-repo.git");
		repository.setHomepage("http://gitlabserver.example.com/test-group/test-repo");
		repository.setDescription("");
		return this;
	}

	public Repository build() {
		return repository;
	}

}
