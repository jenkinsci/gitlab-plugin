package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

public enum GitLabProjectVisibility {
    ALL("all", -1),
    PUBLIC("public", 20),
    INTERNAL("internal", 10),
    PRIVATE("private", 0);

    public static GitLabProjectVisibility byId(String id) {
        for (GitLabProjectVisibility value : values()) {
            if (value.id().equalsIgnoreCase(id)) {
                return value;
            }
        }

        throw new NoSuchElementException("unknown id: " + id);
    }


    public static GitLabProjectVisibility byLevel(int level) {
        for (GitLabProjectVisibility value : values()) {
            if (value.level() == level) {
                return value;
            }
        }

        throw new NoSuchElementException("unknown level: " + level);
    }

    public static Collection<String> ids() {
        Collection<String> ids = new ArrayList<>(values().length);
        for (GitLabProjectVisibility value : values()) {
            ids.add(value.id());
        }

        return ids;
    }

    private final String id;
    private final int level;

    GitLabProjectVisibility(String id, int level) {
        this.id = id;
        this.level = level;
    }

    public String id() {
        return id;
    }

    private int level() {
        return level;
    }
}
