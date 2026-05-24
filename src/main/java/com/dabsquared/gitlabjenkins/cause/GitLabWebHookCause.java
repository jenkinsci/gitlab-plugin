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

    public GitLabWebHookCause(CauseData data) {
        super("");
        this.data = Objects.requireNonNull(data, "data must not be null");
    }

    @Exported
    public CauseData getData() {
        return data;
    }

    @Override
    public String getShortDescription() {
        return data.getShortDescription();
    }
}
