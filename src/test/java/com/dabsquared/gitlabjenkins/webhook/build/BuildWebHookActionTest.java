/*
 * Test tokenMatches in the BuildWebHookAction class
 * Author: Mark Waite
 */
package com.dabsquared.gitlabjenkins.webhook.build;

import static org.junit.jupiter.api.Assertions.*;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.security.ACL;
import org.acegisecurity.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponses.HttpResponseException;

/**
 * Test the BuildWebHookAction class
 *
 * @author Mark Waite
 */
@WithJenkins
class BuildWebHookActionTest {

    private JenkinsRule j;

    private FreeStyleProject project;
    private GitLabPushTrigger trigger;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.get(GitLabConnectionConfig.class).setUseAuthenticatedEndpoint(true);

        project = j.createFreeStyleProject();
        trigger = new GitLabPushTrigger();
        project.addTrigger(trigger);
    }

    // trigger token == action token, expected to succeed
    @Test
    void testNotifierTokenMatches() {
        String triggerToken = "testNotifierTokenMatches-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = triggerToken;
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        action.runNotifier();
        assertTrue(action.performOnPostCalled, "performOnPost not called, token did not match?");
    }

    // trigger token != action token, expected to throw an exception
    @Test
    void testNotifierTokenDoesNotMatchString() {
        String triggerToken = "testNotifierTokenDoesNotMatchString-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = triggerToken + "-no-match"; // Won't match
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        assertThrows(HttpResponseException.class, action::runNotifier);
        assertFalse(action.performOnPostCalled, "performOnPost was called, unexpected token match?");
    }

    // trigger token != null action token, expected to throw an exception
    @Test
    void testNotifierTokenDoesNotMatchNull() {
        String triggerToken = "testNotifierTokenDoesNotMatchNull-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = null;
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        assertThrows(HttpResponseException.class, action::runNotifier);
        assertFalse(action.performOnPostCalled, "performOnPost was called, unexpected token match?");
    }

    // null trigger token != action token, expected to succeed
    @Test
    void testNullNotifierTokenAllowsAccess() {
        // String triggerToken = null;
        // trigger.setSecretToken(triggerToken);
        String actionToken = "testNullNotifierTokenAllowsAccess-token";
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        action.runNotifier();
        assertTrue(action.performOnPostCalled, "performOnPost not called, token did not match?");
    }

    public static class BuildWebHookActionImpl extends BuildWebHookAction {

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
