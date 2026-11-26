package com.dabsquared.gitlabjenkins.cause;

import hudson.triggers.SCMTrigger;
import java.util.Objects;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author Robin Müller
 */
@ExportedBean
public class GitLabWebHookCause extends SCMTrigger.SCMTriggerCause {

    private final CauseData data;
    private final String rawPayload;

    public GitLabWebHookCause(CauseData data) {
        this(data, null);
    }

    public GitLabWebHookCause(CauseData data, String rawPayload) {
        super("");
        this.data = Objects.requireNonNull(data, "data must not be null");
        this.rawPayload = rawPayload;
    }

    @Exported
    public CauseData getData() {
        return data;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    @Override
    public String getShortDescription() {
        return data.getShortDescription();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        GitLabWebHookCause that = (GitLabWebHookCause) other;
        return Objects.equals(data, that.data) && Objects.equals(rawPayload, that.rawPayload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, rawPayload);
    }
}
