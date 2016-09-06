package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import net.sf.json.JSONObject;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public class StatusJsonActionTest extends BuildStatusActionTest {

    @Override
    protected BuildStatusAction getBuildStatusAction(FreeStyleProject project) {
        return new StatusJsonAction(project, commitSha1);
    }

    @Override
    protected void assertSuccessfulBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) {
        JSONObject object = JSONObject.fromObject(new String(out.toByteArray()));
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("success"));
    }

    @Override
    protected void assertFailedBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) {
        JSONObject object = JSONObject.fromObject(new String(out.toByteArray()));
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("failed"));
    }

    @Override
    protected void assertRunningBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) {
        JSONObject object = JSONObject.fromObject(new String(out.toByteArray()));
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("running"));
    }

    @Override
    protected void assertCanceledBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) {
        JSONObject object = JSONObject.fromObject(new String(out.toByteArray()));
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("canceled"));
    }

    @Override
    protected void assertUnstableBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        JSONObject object = JSONObject.fromObject(new String(out.toByteArray()));
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getInt("id"), is(build.getNumber()));
        assertThat(object.getString("status"), is("failed"));
    }

    @Override
    protected void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse response) {
        JSONObject object = JSONObject.fromObject(new String(out.toByteArray()));
        assertThat(object.getString("sha"), is(commitSha1));
        assertThat(object.getString("status"), is("not_found"));
    }
}
