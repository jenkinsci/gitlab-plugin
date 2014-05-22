package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import net.sf.json.JSONObject;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import sun.misc.Cleaner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Triggers a build when we receive a GitHub post-commit webhook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<AbstractProject<?,?>> implements GitLabTrigger {
    @DataBoundConstructor
    public GitLabPushTrigger() {
    }

    /**
     * Called when a POST is made.
     */
    @Deprecated
    public void onPost() {
        onPost("");
    }

    /**
     * Called when a POST is made.
     */
    public void onPost(String triggeredByUser) {
        final String pushBy = triggeredByUser;
        getDescriptor().queue.execute(new Runnable() {
            private boolean runPolling() {
                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFile());

                    try {
                        PrintStream logger = listener.getLogger();
                        long start = System.currentTimeMillis();
                        logger.println("Started on "+ DateFormat.getDateTimeInstance().format(new Date()));
                        boolean result = job.poll(listener).hasChanges();
                        logger.println("Done. Took "+ Util.getTimeSpanString(System.currentTimeMillis() - start));
                        if(result)
                            logger.println("Changes found");
                        else
                            logger.println("No changes");
                        return result;
                    } catch (Error e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                        throw e;
                    } catch (RuntimeException e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                        throw e;
                    } finally {
                        listener.close();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                }
                return false;
            }

            public void run() {
                if (runPolling()) {
                    String name = " #"+job.getNextBuildNumber();
                    GitLabPushCause cause;
                    try {
                        cause = new GitLabPushCause(getLogFile(), pushBy);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to parse the polling log",e);
                        cause = new GitLabPushCause(pushBy);
                    }
                    if (job.scheduleBuild(cause)) {
                        LOGGER.info("SCM changes detected in "+ job.getName()+". Triggering "+name);
                    } else {
                        LOGGER.info("SCM changes detected in "+ job.getName()+". Job is already in the queue");
                    }
                }
            }
        });
    }

    /**
     * Returns the file that records the last/current polling activity.
     */
    public File getLogFile() {
        return new File(job.getRootDir(),"gitlab-polling.log");
    }

    @Override
    public void start(AbstractProject<?,?> project, boolean newInstance) {
        super.start(project, newInstance);
        if (newInstance) {

        }
    }


    @Override
    public void stop() {

    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Hudson.MasterComputer.threadPoolForRemoting);

        private String hookUrl;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return "Build when a change is pushed to GitLab";
        }

        /**
         * Returns the URL that GitLab should post.
         */
        public URL getHookUrl() throws MalformedURLException {
            return hookUrl!=null ? new URL(hookUrl) : new URL(Hudson.getInstance().getRootUrl()+GitLabWebHook.get().getUrlName()+'/');
        }

        public boolean hasOverrideURL() {
            return hookUrl!=null;
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            JSONObject hookMode = json.getJSONObject("hookMode");
            JSONObject o = hookMode.getJSONObject("c");
            if (o!=null && !o.isNullObject()) {
                hookUrl = o.getString("url");
            } else {
                hookUrl = null;
            }
            save();
            return true;
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

        public static boolean allowsHookUrlOverride() {
            return ALLOW_HOOKURL_OVERRIDE;
        }
    }

    /**
     * Set to false to prevent the user from overriding the hook URL.
     */
    public static boolean ALLOW_HOOKURL_OVERRIDE = !Boolean.getBoolean(GitLabPushTrigger.class.getName()+".disableOverride");

    private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());
}
