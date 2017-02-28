package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import org.apache.commons.lang.StringUtils;

import java.util.NoSuchElementException;

public enum GitLabHookEventType {
    MERGE_REQUEST("Merge Request Hook"),
    PUSH("Push Hook"),
    SYSTEM_HOOK("System Hook"),
    TAG_PUSH("Tag Push Hook");

    public static GitLabHookEventType byHeader(String header) {
        if (StringUtils.isBlank(header)) {
            throw new IllegalArgumentException("event-header must not be empty");
        }

        for (GitLabHookEventType value : values()) {
            if (value.header().equals(header)) {
                return value;
            }
        }

        throw new NoSuchElementException("unknown event-header: " + header);
    }

    private final String header;

    GitLabHookEventType(String header) {
        this.header = header;
    }

    private String header() {
        return header;
    }
}
