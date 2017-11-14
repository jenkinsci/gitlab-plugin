package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.google.common.base.Function;
import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class V3GitLabClientBuilder extends ResteasyGitLabClientBuilder {
    private static final int ORDINAL = 2;
    private static final Function<MergeRequest, Integer> MERGE_REQUEST_ID_PROVIDER = new Function<MergeRequest, Integer>() {
        @Override
        public Integer apply(MergeRequest mergeRequest) {
            return mergeRequest.getId();
        }
    };

    public V3GitLabClientBuilder() {
        super(V3GitLabApiProxy.ID, ORDINAL, V3GitLabApiProxy.class, MERGE_REQUEST_ID_PROVIDER);
    }
}
