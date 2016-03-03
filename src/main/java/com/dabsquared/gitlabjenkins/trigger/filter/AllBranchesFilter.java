package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 */
class AllBranchesFilter implements BranchFilter {
    @Override
    public boolean isBranchAllowed(String branchName) {
        return true;
    }

    @Override
    public BranchFilterConfig getConfig() {
        return BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig().build(BranchFilterType.All);
    }
}
