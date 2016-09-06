package com.dabsquared.gitlabjenkins.webhook.status;

import hudson.model.AbstractBuild;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Robin Müller
 */
public abstract class StatusPngActionTest extends BuildStatusActionTest {

    @Override
    protected void assertSuccessfulBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("success.png"))));
    }

    @Override
    protected void assertFailedBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("failed.png"))));
    }

    @Override
    protected void assertRunningBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("running.png"))));
    }

    @Override
    protected void assertCanceledBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unknown.png"))));
    }

    @Override
    protected void assertUnstableBuild(AbstractBuild build, ByteArrayOutputStream out, StaplerResponse response) throws IOException {
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
