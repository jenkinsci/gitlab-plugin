package com.dabsquared.gitlabjenkins.webhook;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import hudson.util.HttpResponses;
import jenkins.triggers.SCMTriggerItem;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
public abstract class BuildStatusAction implements WebHookAction {

    private final AbstractProject<?, ?> project;

    public BuildStatusAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public void execute(StaplerResponse response) {
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(project);
        if (!hasGitSCM(item)) {
            throw HttpResponses.error(409, "The project has no GitSCM configured");
        }
        AbstractBuild<?, ?> build = retrieveBuild(project);
        writeStatusBody(response, build, getStatus(build));
    }

    protected abstract AbstractBuild<?, ?> retrieveBuild(AbstractProject<?, ?> project);

    protected abstract void writeStatusBody(StaplerResponse response, AbstractBuild<?, ?> build, BuildStatus status);

    private boolean hasGitSCM(SCMTriggerItem item) {
        if(item != null) {
            for(SCM scm : item.getSCMs()) {
                if(scm instanceof GitSCM) {
                    return true;
                }
            }
        }
        return false;
    }

    private BuildStatus getStatus(AbstractBuild<?, ?> build) {
        if (build == null) {
            return BuildStatus.PENDING;
        } else if (build.isBuilding()) {
            return BuildStatus.RUNNING;
        } else if (build.getResult() == Result.ABORTED) {
            return BuildStatus.CANCELED;
        } else if (build.getResult() == Result.SUCCESS) {
            return BuildStatus.SUCCESS;
        } else if (build.getResult() == Result.UNSTABLE) {
            return BuildStatus.UNSTABLE;
        } else {
            return BuildStatus.FAILED;
        }
    }

    protected enum BuildStatus {
        PENDING("pending"), RUNNING("running"), CANCELED("canceled"), SUCCESS("success"), FAILED("failed"), UNSTABLE("failed");

        private String value;

        BuildStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
