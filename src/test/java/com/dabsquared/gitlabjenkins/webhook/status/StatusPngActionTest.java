package com.dabsquared.gitlabjenkins.webhook.status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

import hudson.model.FreeStyleBuild;
import java.io.ByteArrayOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
abstract class StatusPngActionTest extends BuildStatusActionTest {

    @Override
    protected void assertSuccessfulBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response)
            throws Exception {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("success.png"))));
    }

    @Override
    protected void assertFailedBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response)
            throws Exception {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("failed.png"))));
    }

    @Override
    protected void assertRunningBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response)
            throws Exception {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("running.png"))));
    }

    @Override
    protected void assertCanceledBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response)
            throws Exception {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unknown.png"))));
    }

    @Override
    protected void assertUnstableBuild(FreeStyleBuild build, ByteArrayOutputStream out, StaplerResponse2 response)
            throws Exception {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unstable.png"))));
    }

    @Override
    protected void assertNotFoundBuild(ByteArrayOutputStream out, StaplerResponse2 response) throws Exception {
        verify(response).setHeader("Expires", "Fri, 01 Jan 1984 00:00:00 GMT");
        verify(response).setHeader("Cache-Control", "no-cache, private");
        verify(response).setHeader("Content-Type", "image/png");
        assertThat(out.toByteArray(), is(IOUtils.toByteArray(getClass().getResourceAsStream("unknown.png"))));
    }
}
