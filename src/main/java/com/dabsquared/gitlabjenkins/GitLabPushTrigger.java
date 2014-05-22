package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
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
            private boolean polling() {
                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFile());

                    try {
                        PrintStream logger = listener.getLogger();

                        long start = System.currentTimeMillis();
                        logger.println("Started on " + DateFormat.getDateTimeInstance().format(new Date()));
                        boolean result = job.poll(listener).hasChanges();
                        logger.println("Done. Took " + Util.getTimeSpanString(System.currentTimeMillis() - start));

                        if (result) {
                            logger.println("Changes found");
                        } else {
                            logger.println("No changes");
                        }

                        return result;
                    } catch (Error e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.log(Level.SEVERE, "Failed to record SCM polling", e);
                        throw e;
                    } catch (RuntimeException e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.log(Level.SEVERE, "Failed to record SCM polling", e);
                        throw e;
                    } finally {
                        listener.closeQuietly();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to record SCM polling", e);
                }

                return false;
            }

            public void run() {
                LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                if (polling()) {
                    String name = " #" + job.getNextBuildNumber();
                    GitLabPushCause cause = createGitLabPushCause(req);
                    if (job.scheduleBuild(job.getQuietPeriod(), cause)) {
                        LOGGER.log(Level.INFO, "SCM changes detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                    } else {
                        LOGGER.log(Level.INFO, "SCM changes detected in {0}. Job is already in the queue.", job.getName());
                    }
                }
            }

            private GitLabPushCause createGitLabPushCause(GitLabPushRequest req) {
                GitLabPushCause cause;
                String triggeredByUser = req.getPusher().getName();
                try {
                    cause = new GitLabPushCause(triggeredByUser, getLogFile());
                } catch (IOException ex) {
                    cause = new GitLabPushCause(triggeredByUser);
                }
                return cause;
            }

        });
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singletonList(new GitLabWebHookPollingAction());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.get();
    }

    public File getLogFile() {
        return new File(job.getRootDir(), "gitlab-polling.log");
    }

    private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());


    public class GitLabWebHookPollingAction implements Action {

        public AbstractProject<?, ?> getOwner() {
            return job;
        }

        public String getIconFileName() {
            return "/plugin/gitlab/images/24x24/gitlab-log.png";
        }

        public String getDisplayName() {
            return "GitLab Hook Log";
        }

        public String getUrlName() {
            return "GitLabPollLog";
        }

        public String getLog() throws IOException {
            return Util.loadFile(getLogFile());
        }

        public void writeLogTo(XMLOutput out) throws IOException {
            new AnnotatedLargeText<GitLabWebHookPollingAction>(
                    getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
        }
    }

    public static class GitLabPushCause extends SCMTriggerCause {

        private final String pushedBy;

        public GitLabPushCause(String pushedBy) {
            this.pushedBy = pushedBy;
        }

        public GitLabPushCause(String pushedBy, File logFile) throws IOException {
            super(logFile);
            this.pushedBy = pushedBy;
        }

        public GitLabPushCause(String pushedBy, String pollingLog) {
            super(pollingLog);
            this.pushedBy = pushedBy;
        }

        @Override
        public String getShortDescription() {
            if (pushedBy == null) {
                return "Started by GitLab push";
            } else {
                return String.format("Started by GitLab push by %s", pushedBy);
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return "Build when a change is pushed to GitLab";
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
