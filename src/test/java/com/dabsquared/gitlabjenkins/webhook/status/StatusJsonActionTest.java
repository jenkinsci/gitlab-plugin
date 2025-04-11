package com.dabsquared.gitlabjenkins.webhook.status;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import java.io.ByteArrayOutputStream;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class StatusJsonActionTest extends BuildStatusActionTest {

    @Override
    protected BuildStatusAction getBuildStatusAction(FreeStyleProject project) {
        return new StatusJsonAction(project, commitSha1);
    }

    @Override
    protected void assertSuccessfulBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) {
        JSONObject object = JSONObject.fromObject(out.toString());
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("success"));
    }

    @Override
    protected void assertFailedBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) {
        JSONObject object = JSONObject.fromObject(out.toString());
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("failed"));
    }

    @Override
    protected void assertRunningBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) {
        JSONObject object = JSONObject.fromObject(out.toString());
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("running"));
    }

    @Override
    protected void assertCanceledBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) {
        JSONObject object = JSONObject.fromObject(out.toString());
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("canceled"));
    }

    @Override
    protected void assertUnstableBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response) {
        JSONObject object = JSONObject.fromObject(out.toString());
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("failed"));
    }

    @Override
    protected void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse2 response) {
        JSONObject object = JSONObject.fromObject(out.toString());
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getString("status"), is("not_found"));
    }
}
