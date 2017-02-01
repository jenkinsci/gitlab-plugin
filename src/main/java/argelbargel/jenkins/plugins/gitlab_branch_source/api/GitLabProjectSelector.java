package argelbargel.jenkins.plugins.gitlab_branch_source.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

public enum GitLabProjectSelector {
    OWNED("owned"),
    STARRED("starred"),
    VISIBLE("visible");

    public static GitLabProjectSelector byId(String id) {
        for (GitLabProjectSelector value : values()) {
            if (value.id().equalsIgnoreCase(id)) {
                return value;
            }
        }

        throw new NoSuchElementException("unknown id: " + id);
    }

    public static Collection<String> ids() {
        Collection<String> ids = new ArrayList<>(values().length);
        for (GitLabProjectSelector value : values()) {
            ids.add(value.id());
        }

        return ids;
    }

    private final String id;

    GitLabProjectSelector(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
