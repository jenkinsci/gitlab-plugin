package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import java.util.List;

/**
 * @author Roland Hauser
 */
public interface CommitSource {

    List<Commit> getCommits();
}
