package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.init.Terminator;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;


/**
 * Publishes Build-Status to GitLab using separate threads so it does not block while sending messages
 * TODO: Multi-Threading is easy to get wrong and wreak havoc. Check if there is no better way to do this built into Jenkins
 */
public class GitLabSCMBuildStatusPublisher {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMBuildStatusPublisher.class.getName());

    private static GitLabSCMBuildStatusPublisher instance;

    static GitLabSCMBuildStatusPublisher instance() {
        if (instance == null) {
            instance = new GitLabSCMBuildStatusPublisher();
        }

        return instance;
    }

    @Terminator
    public static void terminate() throws InterruptedException {
        if (instance != null) {
            instance.shutdown();
        }
    }

    private final ExecutorService executorService;

    private GitLabSCMBuildStatusPublisher() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void publish(Run<?, ?> build, String publisherName, int projectId, String ref, String hash, BuildState state, String description) {
        executorService.execute(new Message(build, publisherName, projectId, ref, hash, state, description));
    }


    private void shutdown() throws InterruptedException {
        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);
    }


    private static final class Message implements Runnable {
        private final Run<?, ?> build;
        private final int projectId;
        private final String ref;
        private final String hash;
        private final BuildState state;
        private final String context;
        private final String description;


        private Message(Run<?, ?> build, String context, int projectId, String ref, String hash, BuildState state, String description) {
            this.build = build;
            this.projectId = projectId;
            this.ref = ref;
            this.hash = hash;
            this.state = state;
            this.context = context;
            this.description = description;
        }

        @Override
        public void run() {
            GitLabApi client = GitLabConnectionProperty.getClient(build);
            if (client == null) {
                LOGGER.log(WARNING, "cannot publish build-status pending as no gitlab-connection is configured!");
            } else {
                try {
                    client.changeBuildStatus(projectId, hash, state, ref, context,
                            Jenkins.getInstance().getRootUrl() + build.getUrl() + build.getNumber(), description);
                } catch (Exception e) {
                    LOGGER.log(SEVERE, "failed to set build-status of '" + context + "' for project " + projectId + " to " + state, e);
                }
            }
        }
    }
}
