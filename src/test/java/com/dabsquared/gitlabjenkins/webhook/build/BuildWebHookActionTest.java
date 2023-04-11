/*
 * Test tokenMatches in the BuildWebHookAction class
 * Author: Mark Waite
 */
package com.dabsquared.gitlabjenkins.webhook.build;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.security.ACL;
import org.acegisecurity.Authentication;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses.HttpResponseException;

/**
 * Test the BuildWebHookAction class
 *
 * @author Mark Waite
 */
public class BuildWebHookActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private FreeStyleProject project;
    private GitLabPushTrigger trigger;

    public BuildWebHookActionTest() {}

    @Before
    public void confgureGitLabConnection() throws Exception {
        j.get(GitLabConnectionConfig.class).setUseAuthenticatedEndpoint(true);
    }

    @Before
    public void createFreeStyleProjectWithGitLabTrigger() throws Exception {
        project = j.createFreeStyleProject();
        trigger = new GitLabPushTrigger();
        project.addTrigger(trigger);
    }

    // trigger token == action token, expected to succeed
    @Test
    public void testNotifierTokenMatches() throws Exception {
        String triggerToken = "testNotifierTokenMatches-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = triggerToken;
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        action.runNotifier();
        assertTrue("performOnPost not called, token did not match?", action.performOnPostCalled);
    }

    // trigger token != action token, expected to throw an exception
    @Test
    public void testNotifierTokenDoesNotMatchString() throws Exception {
        String triggerToken = "testNotifierTokenDoesNotMatchString-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = triggerToken + "-no-match"; // Won't match
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        assertThrows(HttpResponseException.class, () -> {
            action.runNotifier();
        });
        assertFalse("performOnPost was called, unexpected token match?", action.performOnPostCalled);
    }

    // trigger token != null action token, expected to throw an exception
    @Test
    public void testNotifierTokenDoesNotMatchNull() throws Exception {
        String triggerToken = "testNotifierTokenDoesNotMatchNull-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = null;
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        assertThrows(HttpResponseException.class, () -> {
            action.runNotifier();
        });
        assertFalse("performOnPost was called, unexpected token match?", action.performOnPostCalled);
    }

    // null trigger token != action token, expected to succeed
    @Test
    public void testNullNotifierTokenAllowsAccess() throws Exception {
        // String triggerToken = null;
        // trigger.setSecretToken(triggerToken);
        String actionToken = "testNullNotifierTokenAllowsAccess-token";
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        action.runNotifier();
        assertTrue("performOnPost not called, token did not match?", action.performOnPostCalled);
    }

    public class BuildWebHookActionImpl extends BuildWebHookAction {

        // Used for the assertion that tokenMatches() returned true
        public boolean performOnPostCalled = false;

        private final MyTriggerNotifier myNotifier;

        public BuildWebHookActionImpl() {
            myNotifier = new MyTriggerNotifier(null, null, null);
        }

        public BuildWebHookActionImpl(@NonNull Project project, @NonNull String token) {
            myNotifier = new MyTriggerNotifier(project, token, ACL.SYSTEM);
        }

        public void runNotifier() {
            myNotifier.run();
        }

        public class MyTriggerNotifier extends TriggerNotifier {

            public MyTriggerNotifier(Item project, String secretToken, Authentication authentication) {
                super(project, secretToken, authentication);
            }

            @Override
            protected void performOnPost(GitLabPushTrigger trigger) {
                performOnPostCalled = true;
            }
        }

        @Override
        public void processForCompatibility() {}

        @Override
        public void execute() {}
    }
}
