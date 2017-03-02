package argelbargel.jenkins.plugins.gitlab_branch_source;


import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPI;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProject;

import java.util.List;


public class GroupProjectQuery extends ProjectQuery {
    private final String group;

    GroupProjectQuery(String group, String selector, String visibility, String searchPattern) {
        super(selector, visibility, searchPattern);
        this.group = group;
    }

    @Override
    protected List<GitLabProject> execute(GitLabAPI api) throws GitLabAPIException {
        return api.findProjects(group, getSelector(), getVisibility(), getSearchPattern());
    }
}
