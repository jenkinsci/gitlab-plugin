package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.data.LastCommit;
import com.dabsquared.gitlabjenkins.data.ObjectAttributes;
import com.dabsquared.gitlabjenkins.webhook.build.MergeRequestBuildAction;
import com.dabsquared.gitlabjenkins.webhook.build.PushBuildAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.BranchStatusPngAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitBuildPageRedirectAction;
import com.dabsquared.gitlabjenkins.webhook.status.StatusJsonAction;
import com.dabsquared.gitlabjenkins.webhook.status.CommitStatusPngAction;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;
import hudson.security.ACL;
import hudson.security.csrf.CrumbExclusion;
import hudson.triggers.Trigger;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel Brooks
 */

@Extension
public class GitLabWebHook implements UnprotectedRootAction {

    private static final Logger LOGGER = Logger.getLogger(GitLabWebHook.class.getName());

    public static final String WEBHOOK_URL = "project";

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return WEBHOOK_URL;
    }

    public void getDynamic(final String projectName, final StaplerRequest req, StaplerResponse res) {
        LOGGER.log(Level.INFO, "WebHook called with url: {0}", req.getRestOfPath());
        final Iterator<String> restOfPathParts = Splitter.on('/').omitEmptyStrings().split(req.getRestOfPath()).iterator();
        final AbstractProject<?, ?>[] projectHolder = new AbstractProject<?, ?>[] { null };
        ACL.impersonate(ACL.SYSTEM, new Runnable() {

            public void run() {
                final Jenkins jenkins = Jenkins.getInstance();
                if (jenkins != null) {
                    Item item = jenkins.getItemByFullName(projectName);
                    while (item instanceof ItemGroup<?> && !(item instanceof AbstractProject<?, ?>) && restOfPathParts.hasNext()) {
                        item = jenkins.getItem(restOfPathParts.next(), (ItemGroup<?>) item);
                    }
                    if (item instanceof AbstractProject<?, ?>) {
                        projectHolder[0] = (AbstractProject<?, ?>) item;
                    }
                }
            }

        });

        final AbstractProject<?, ?> project = projectHolder[0];
        if (project == null) {
            throw HttpResponses.notFound();
        }

        final List<String> paths = new ArrayList<String>();
        while (restOfPathParts.hasNext()) {
            paths.add(restOfPathParts.next());
        }

        /*
         * Since GitLab 7.10 the URL contains the pushed branch name.
         * Extract and store the branch name for further processing.
         * http://jenkins.host.com/project/<ProjectName>/refs/<branchName>/commit/<SHA1>
         */
        String sourceBranch = null;
        if (!paths.isEmpty() && paths.get(0).equals("refs")) {
            int index = paths.lastIndexOf("commits");
            if (index == -1)
                index = paths.lastIndexOf("builds");
            if (index == -1)
                index = paths.lastIndexOf("!builds");
            
            if (index > 1) {
                sourceBranch = Joiner.on('/').join(paths.subList(1, index)); // extract branch
                paths.subList(0, index).clear(); // remove 'refs/<branchName>'
            }
        }

        String token = req.getParameter("token");

        //TODO: Check token authentication with project id. For now we are not using this.

        String requestBody;
        try {
            requestBody = IOUtils.toString(req.getInputStream());
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to read request body");
        }

        if(paths.size() == 0) {
        	if (req.hasParameter("ref")){
                new BranchBuildPageRedirectAction(project, req.getParameter("ref")).execute(res);
        	} else {
        		this.generateBuild(requestBody, project, req, res);
        	}
        	throw HttpResponses.ok();
        }

        String lastPath = paths.get(paths.size()-1);
        String firstPath = paths.get(0);
        if(lastPath.equals("status.json") && !firstPath.equals("!builds")) {
            new StatusJsonAction(project, paths.get(1)).execute(res);
        } else if(lastPath.equals("build") || (lastPath.equals("status.json") && firstPath.equals("!builds"))) {
            this.generateBuild(requestBody, project, req, res);
        } else if(lastPath.equals("status.png")) {
            if (req.hasParameter("ref")) {
                new BranchStatusPngAction(project, req.getParameter("ref")).execute(res);
            } else {
                new CommitStatusPngAction(project, req.getParameter("sha1")).execute(res);
            }
        } else if((firstPath.equals("commits") || firstPath.equals("builds")) && !lastPath.equals("status.json")) {
            new CommitBuildPageRedirectAction(project, lastPath).execute(res);
        } else{
            LOGGER.warning("Dynamic request mot met: First path: '" + firstPath + "' late path: '" + lastPath + "'");
        }

        throw HttpResponses.ok();

    }

    /**
     * Take the GitLab Data and parse through it.
     * {
     #     "before": "95790bf891e76fee5e1747ab589903a6a1f80f22",
     #     "after": "da1560886d4f094c3e6c9ef40349f7d38b5d27d7",
     #     "ref": "refs/heads/master",
     #     "commits": [
     #       {
     #         "id": "b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327",
     #         "message": "Update Catalan translation to e38cb41.",
     #         "timestamp": "2011-12-12T14:27:31+02:00",
     #         "url": "http://localhost/diaspora/commits/b6568db1bc1dcd7f8b4d5a946b0b91f9dacd7327",
     #         "author": {
     #           "name": "Jordi Mallach",
     #           "email": "jordi@softcatala.org",
     #         }
     #       }, .... more commits
     #     ]
     #   }
     * @param data
     * @param req
     */
    private void generateBuild(String data, AbstractProject<?, ?> project, StaplerRequest req, StaplerResponse response) {
        String eventHeader = req.getHeader("X-Gitlab-Event");
        if(eventHeader.equals("Merge Request Hook")) {
            new MergeRequestBuildAction(project, data).execute(response);
        } else if (eventHeader.equals("Push Hook")) {
            new PushBuildAction(project, data).execute(response);
        }
    }

    @Extension
    public static class GitlabWebHookCrumbExclusion extends CrumbExclusion {

        @Override
        public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.startsWith(getExclusionPath())) {
                chain.doFilter(req, resp);
                return true;
            }
            return false;
        }

        private String getExclusionPath() {
            return '/' + WEBHOOK_URL + '/';
        }
    }
}
