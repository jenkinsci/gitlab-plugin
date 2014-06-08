package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.plugins.git.RevisionParameterAction;
import hudson.triggers.SCMTrigger;
import hudson.triggers.SCMTrigger.SCMTriggerCause;

import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;

import jenkins.model.Jenkins.MasterComputer;

import org.apache.commons.jelly.XMLOutput;

import com.dabsquared.gitlabjenkins.GitLabPushRequest.Commit;

/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<AbstractProject<?, ?>> {


    @DataBoundConstructor
    public GitLabPushTrigger() {

    }

    public void onPost(final GitLabPushRequest req) {
        getDescriptor().queue.execute(new Runnable() {

            public void run() {
                LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                String name = " #" + job.getNextBuildNumber();
                GitLabPushCause cause = createGitLabPushCause(req);
                Action[] actions = createActions(req);
                if (job.scheduleBuild(job.getQuietPeriod(), cause, actions)) {
                    LOGGER.log(Level.INFO, "GitLab Push detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                } else {
                    LOGGER.log(Level.INFO, "GitLab Push detected in {0}. Job is already in the queue.", job.getName());
                }
            }

            private GitLabPushCause createGitLabPushCause(GitLabPushRequest req) {
                GitLabPushCause cause;
                String triggeredByUser = req.getCommits().get(0).getAuthor().getName();
                try {
                    cause = new GitLabPushCause(triggeredByUser, getLogFile());
                } catch (IOException ex) {
                    cause = new GitLabPushCause(triggeredByUser);
                }
                return cause;
            }

            private Action[] createActions(GitLabPushRequest req) {
                List<Action> actions = new ArrayList<Action>();

                Commit lastCommit = req.getLastCommit();
                actions.add(new RevisionParameterAction(lastCommit.getId(), false));

                return actions.toArray(new Action[0]);
            }

        });
    }

    public void onPost(final GitLabMergeRequest req) {
        getDescriptor().queue.execute(new Runnable() {
            public void run() {
                LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                String name = " #" + job.getNextBuildNumber();
                GitLabMergeCause cause = createGitLabMergeCause(req);
                if (job.scheduleBuild(job.getQuietPeriod(), cause)) {
                    LOGGER.log(Level.INFO, "GitLab Merge Request detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                } else {
                    LOGGER.log(Level.INFO, "GitLab Merge Request detected in {0}. Job is already in the queue.", job.getName());
                }
            }

            private GitLabMergeCause createGitLabMergeCause(GitLabMergeRequest req) {
                GitLabMergeCause cause;
                String triggeredByUser = req.getObjectAttribute().getAuthorId() + "";
                try {
                    cause = new GitLabMergeCause(triggeredByUser, getLogFile());
                } catch (IOException ex) {
                    cause = new GitLabMergeCause(triggeredByUser);
                }
                return cause;
            }

        });
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.get();
    }

    public File getLogFile() {
        return new File(job.getRootDir(), "gitlab-polling.log");
    }

    private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());



    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        AbstractProject project;

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        @Override
        public boolean isApplicable(Item item) {
            if(item instanceof AbstractProject) {
                project = (AbstractProject) item;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getDisplayName() {
            if(project == null) {
                return "Build when a change is pushed to GitLab, unknown URL";
            }

            String projectURL = null;

            try {
                projectURL = URLEncoder.encode(project.getName(), "UTF-8");
                projectURL = projectURL.replace("+", "%20");
            } catch (UnsupportedEncodingException e) {
                projectURL = project.getName();
            }

            return "Build when a change is pushed to GitLab. GitLab CI Service URL: " + Jenkins.getInstance().getRootUrl() + "project/" + projectURL;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-jenkins/help/help-trigger.jelly";
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
