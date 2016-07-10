package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.HttpResponses;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Robin Müller
 */
public class StatusJsonAction extends BuildStatusAction {

    private String sha1;

    public StatusJsonAction(Job<?, ?> project, String sha1) {
        super(project, BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, sha1));
        this.sha1 = sha1;
    }

    @Override
    protected void writeStatusBody(StaplerResponse response, Run<?, ?> build, BuildStatus status) {
        GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(build.getParent());
        String statusValue = status.getValue();

        if (status == BuildStatus.UNSTABLE && trigger.getMarkBuildUnstableAsSuccess()) {
            statusValue = BuildStatus.SUCCESS.getValue();
        }

        try {
            JSONObject object = new JSONObject();
            object.put("sha", sha1);
            if (build != null) {
                object.put("id", build.getNumber());
            }
            object.put("status", statusValue);
            writeBody(response, object);
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to generate response");
        }
    }

    private void writeBody(StaplerResponse response, JSONObject body) throws IOException {
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(body.toString());
        writer.flush();
        writer.close();
    }
}
