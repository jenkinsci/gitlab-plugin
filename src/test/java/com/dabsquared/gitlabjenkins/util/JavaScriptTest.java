package com.dabsquared.gitlabjenkins.util;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.xml.sax.SAXException;

/**
 * JS in HTMLUnit isn't great, so ensure our JS doesn't cause it to fall over.
 */
@WithJenkins
public class JavaScriptTest {
    @Test
    void testJavaScriptExecution(JenkinsRule j) throws IOException, SAXException {
        final FreeStyleProject fs = j.createFreeStyleProject();
        final GitLabPushTrigger trigger = new GitLabPushTrigger();
        fs.addTrigger(trigger);
        fs.save();
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.getPage(fs, "configure");
        }
    }
}
