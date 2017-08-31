package com.dabsquared.gitlabjenkins.webhook;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.UnprotectedRootAction;
import hudson.security.csrf.CrumbExclusion;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.NoteHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PipelineHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.util.GitSCMUtil;
import com.dabsquared.gitlabjenkins.util.JsonUtil;



import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Brooks
 */

@Extension
public class GitLabWebHook implements UnprotectedRootAction {

    public static final String WEBHOOK_URL = "project";

    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return WEBHOOK_URL;
    }

    public void getDynamic(final String projectName, final StaplerRequest request, StaplerResponse response) throws IOException {
        boolean globallyTriggered = false;
        ActionResolver actionResolver = new ActionResolver (request);
        List<GitLabConnection> matchingGitLabConnections;
        WebHook webhook = null;

        LOGGER.log (Level.INFO, "WebHook called with url: {0}, projectname: " + projectName, request.getRequestURIWithQueryString ());
        LOGGER.log (Level.FINE, "Webhook: " + JsonUtil.toPrettyPrint (actionResolver.getRequestBody ()));
        switch (request.getHeader ("X-Gitlab-Event")) {
            case "Merge Request Hook":
                webhook = JsonUtil.readFromStaplerRequest (request, MergeRequestHook.class);
                break;
            case "Push Hook":
            case "Tag Push Hook":
                webhook = JsonUtil.readFromStaplerRequest (request, PushHook.class);
                break;
            case "Note Hook":
                webhook = JsonUtil.readFromStaplerRequest (request, NoteHook.class);
                break;
            case "Pipeline Hook":
                webhook = JsonUtil.readFromStaplerRequest(request, PipelineHook.class);
                break;
            default:
                actionResolver.resolve(projectName, request).execute(response);
                return;
        }

        matchingGitLabConnections = webhook instanceof MergeRequestHook ? getConfiguredGlobalWebhookConnections(projectName, ((MergeRequestHook)webhook).getObjectAttributes ().getTarget ().getHomepage ())
                                                                        : getConfiguredGlobalWebhookConnections(projectName, webhook.getProject ().getHomepage ());

        if (matchingGitLabConnections != null && !matchingGitLabConnections.isEmpty ()) {
            LOGGER.log (Level.INFO, "found at least one configured global webhook for url: {0}", request.getRequestURIWithQueryString ());


            for (Job job: Jenkins.getInstance ().getAllItems (Job.class)) {
                GitLabConnectionProperty jobGitLabConnectionProperty = (GitLabConnectionProperty) job.getProperty (GitLabConnectionProperty.class);
                if (jobGitLabConnectionProperty == null) {
                    continue;
                }
                for (GitLabConnection gitLabConnection : matchingGitLabConnections){
                    if (gitLabConnection.getName ().equals (jobGitLabConnectionProperty.getGitLabConnection ())) {
                        if (GitSCMUtil.isConfiguredGitRepository (job, webhook)) {
                            LOGGER.log (Level.FINE, "global trigger matched for job: {0}", job.getFullDisplayName ());
                            actionResolver.resolve (job, request).executeNoResponse (response);
                            globallyTriggered = true;
                        }
                    }
                }
            }
        }
        if (!globallyTriggered) {
            actionResolver.resolve(projectName, request).execute(response);
        } else {
            throw HttpResponses.ok();
        }
    }

    /**
     *
     * @param projectName Part of the trigger URL to compare with configured global webhook URLs
     * @return List of GitLabConnection which have {@param projectName} configured as their global webhook URL.
     */
    private List<GitLabConnection> getConfiguredGlobalWebhookConnections (String projectName, String gitProjectHomepage) {
        List<GitLabConnection> matchingConnections = new ArrayList<GitLabConnection>();
        GitLabConnectionConfig gitLabConnectionConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
        if (gitLabConnectionConfig != null && gitProjectHomepage != null){
            for (GitLabConnection gitLabConnection : gitLabConnectionConfig.getConnections ()){
                try {
                    if (gitLabConnection.getGlobalWebhookURL ().equals (projectName) && gitProjectHomepage.contains (new URL(gitLabConnection.getUrl ()).getHost())){
                        matchingConnections.add (gitLabConnection);
                    }
                } catch (MalformedURLException e) {
                    // not actually an issue for us at this point, should still be logged since it hints at a bogus configuration
                    LOGGER.log (Level.WARNING, "Malformed URL found in gitLabConnectionConfig: " + e.getMessage ());
                }
            }
        } else if (gitProjectHomepage == null) {
            LOGGER.log (Level.INFO, "No project homepage in webhook");
        }
        return matchingConnections;
    }

    @Extension
    public static class GitlabWebHookCrumbExclusion extends CrumbExclusion {

        @Override
        public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith('/' + WEBHOOK_URL + '/')) {
                chain.doFilter(req, resp);
                return true;
            }
            return false;
        }
    }
}
