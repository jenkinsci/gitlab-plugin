package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public final class BranchFilterConfig {

    private final BranchFilterType type;
    private final String includeBranchesSpec;
    private final String excludeBranchesSpec;
    private final String targetBranchRegex;

    private BranchFilterConfig(BranchFilterType type, String includeBranchesSpec, String excludeBranchesSpec, String targetBranchRegex) {
        this.type = type;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.targetBranchRegex = targetBranchRegex;
    }

    public BranchFilterType getType() {
        return type;
    }

    public String getIncludeBranchesSpec() {
        return includeBranchesSpec;
    }

    public String getExcludeBranchesSpec() {
        return excludeBranchesSpec;
    }

    public String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    public static class BranchFilterConfigBuilder {
        private String includeBranchesSpec;
        private String excludeBranchesSpec;
        private String targetBranchRegex;

        public static BranchFilterConfigBuilder branchFilterConfig() {
            return new BranchFilterConfigBuilder();
        }

        public BranchFilterConfigBuilder withIncludeBranchesSpec(String includeBranchesSpec) {
            this.includeBranchesSpec = includeBranchesSpec;
            return this;
        }

        public BranchFilterConfigBuilder withExcludeBranchesSpec(String excludeBranchesSpec) {
            this.excludeBranchesSpec = excludeBranchesSpec;
            return this;
        }

        public BranchFilterConfigBuilder withTargetBranchRegex(String targetBranchRegex) {
            this.targetBranchRegex = targetBranchRegex;
            return this;
        }

        public BranchFilterConfig build(BranchFilterType type) {
            return new BranchFilterConfig(type, includeBranchesSpec, excludeBranchesSpec, targetBranchRegex);
        }
    }
}
