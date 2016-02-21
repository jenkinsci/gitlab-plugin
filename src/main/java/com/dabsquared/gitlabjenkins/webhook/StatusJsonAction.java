package com.dabsquared.gitlabjenkins.webhook;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.util.HttpResponses;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Robin Müller
 */
public class StatusJsonAction extends BuildStatusAction {

    private String commitSHA1;

    public StatusJsonAction(AbstractProject<?, ?> project, String commitSHA1) {
        super(project);
        this.commitSHA1 = commitSHA1;
    }

    @Override
    protected void writeStatusBody(StaplerResponse response, AbstractBuild<?, ?> build, BuildStatus status) {
        try {
            JSONObject object = new JSONObject();
            object.put("sha", commitSHA1);
            if (build != null) {
                object.put("id", build.getNumber());
            }
            object.put("status", status.getValue());
            writeBody(response, object);
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to generate response");
        }
    }

    @Override
    protected AbstractBuild<?, ?> retrieveBuild(AbstractProject<?, ?> project) {
        for (AbstractBuild build : project.getBuilds()) {
            BuildData data = build.getAction(BuildData.class);
            if (data != null && data.lastBuild != null) {
                if (data.lastBuild.isFor(commitSHA1)) {
                    return build;
                }
            }
        }
        return null;
    }

    private void writeBody(StaplerResponse response, JSONObject body) throws IOException {
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(body.toString());
        writer.flush();
        writer.close();
    }
}
