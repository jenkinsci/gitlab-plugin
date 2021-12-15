package com.dabsquared.gitlabjenkins.webhook.status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

import hudson.model.FreeStyleBuild;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
public abstract class StatusPngActionTest extends BuildStatusActionTest {

    @Override
    protected void assertSuccessfulBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("success.png"))));
    }

    @Override
    protected void assertFailedBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("failed.png"))));
    }

    @Override
    protected void assertRunningBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("running.png"))));
    }

    @Override
    protected void assertCanceledBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unknown.png"))));
    }

    @Override
    protected void assertUnstableBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unstable.png"))));
    }

    @Override
    protected void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unknown.png"))));
    }
}
