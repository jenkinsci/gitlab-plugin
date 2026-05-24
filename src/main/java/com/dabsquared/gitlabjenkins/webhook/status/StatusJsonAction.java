package com.dabsquared.gitlabjenkins.webhook.status;

import com.dabsquared.gitlabjenkins.util.BuildUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.io.PrintWriter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerResponse2;

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
    protected void writeStatusBody(StaplerResponse2 response, Run<?, ?> build, BuildStatus status) {
        try {
            JSONObject object = new JSONObject();
            object.put("sha", sha1);
            if (build != null) {
                object.put("id", build.getNumber());
            }
            object.put("status", status.getValue());
            writeBody(response, object);
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to generate response");
        }
    }

    private void writeBody(StaplerResponse2 response, JSONObject body) throws IOException {
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(body.toString());
        writer.flush();
        writer.close();
    }
}
