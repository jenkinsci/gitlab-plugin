package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public final class BranchFilterConfig {

    private final BranchFilterType type;
    private final String includeBranchesSpec;
    private final String excludeBranchesSpec;
    private final String sourceBranchRegex;
    private final String targetBranchRegex;

    private BranchFilterConfig(BranchFilterType type, String includeBranchesSpec, String excludeBranchesSpec, String sourceBranchRegex, String targetBranchRegex) {
        this.type = type;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.sourceBranchRegex = sourceBranchRegex;
        this.targetBranchRegex = targetBranchRegex;
    }

    public BranchFilterType getType() {
        return type;
    }

    String getIncludeBranchesSpec() {
        return includeBranchesSpec;
    }

    String getExcludeBranchesSpec() {
        return excludeBranchesSpec;
    }

    String getSourceBranchRegex() {
        return sourceBranchRegex;
    }

    String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    public static class BranchFilterConfigBuilder {
        private String includeBranchesSpec;
        private String excludeBranchesSpec;
        private String sourceBranchRegex;
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

        public BranchFilterConfigBuilder withSourceBranchRegex(String sourceBranchRegex) {
            this.sourceBranchRegex = sourceBranchRegex;
            return this;
        }

        public BranchFilterConfigBuilder withTargetBranchRegex(String targetBranchRegex) {
            this.targetBranchRegex = targetBranchRegex;
            return this;
        }

        public BranchFilterConfig build(BranchFilterType type) {
            return new BranchFilterConfig(type, includeBranchesSpec, excludeBranchesSpec, sourceBranchRegex, targetBranchRegex);
        }
    }
}
