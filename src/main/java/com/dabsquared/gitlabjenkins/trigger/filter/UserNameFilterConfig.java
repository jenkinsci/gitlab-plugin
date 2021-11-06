package com.dabsquared.gitlabjenkins.trigger.filter;

/**
 * @author jean flores
 */
public final class UserNameFilterConfig {

    private final UserNameFilterType type;
    private final String excludeUserNamesSpec;

    private UserNameFilterConfig(UserNameFilterType type, String excludeUserNamesSpec) {
        this.type = type;
        this.excludeUserNamesSpec = excludeUserNamesSpec;
    }

    public UserNameFilterType getType() {
        return type;
    }

    String getExcludeUserNamesSpec() {
        return excludeUserNamesSpec;
    }

    public static class UserNameFilterConfigBuilder {
        private String excludeUserNamesSpec;

        public static UserNameFilterConfigBuilder userNameFilterConfig() {
            return new UserNameFilterConfigBuilder();
        }

        public UserNameFilterConfigBuilder withExcludeUserNamesSpec(String excludeUserNamesSpec) {
            this.excludeUserNamesSpec = excludeUserNamesSpec;
            return this;
        }

        public UserNameFilterConfig build(UserNameFilterType type) {
            return new UserNameFilterConfig(type, excludeUserNamesSpec);
        }
    }
}
