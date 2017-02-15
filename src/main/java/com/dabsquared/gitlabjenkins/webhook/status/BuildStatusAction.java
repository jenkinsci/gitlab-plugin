package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.SCM;
import hudson.util.HttpResponses;
import jenkins.triggers.SCMTriggerItem;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
abstract class BuildStatusAction implements WebHookAction {

    private final Job<?, ?> project;
    private Run<?, ?> build;

    protected BuildStatusAction(Job<?, ?> project, Run<?, ?> build) {
        this.project = project;
        this.build = build;
    }

    public void execute(StaplerResponse response) {
        writeStatusBody(response, build, getStatus(build));
    }

    protected abstract void writeStatusBody(StaplerResponse response, Run<?, ?> build, BuildStatus status);

   
    private BuildStatus getStatus(Run<?, ?> build) {
        if (build == null) {
            return BuildStatus.NOT_FOUND;
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
        NOT_FOUND("not_found"), RUNNING("running"), CANCELED("canceled"), SUCCESS("success"), FAILED("failed"), UNSTABLE("failed");

        private String value;

        BuildStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
