package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.api.GitLabApi;
import com.dabsquared.gitlabjenkins.gitlab.api.model.MergeRequest;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Project;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.branch.MultiBranchProject;
import jenkins.model.Jenkins;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.util.JsonUtil.toPrettyPrint;
import static com.dabsquared.gitlabjenkins.util.LoggerUtil.toArray;

/**
 * @author Robin Müller
 */
public class PushBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final Item project;
    private PushHook pushHook;
    private final String secretToken;

    public PushBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = JsonUtil.read(json, PushHook.class);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // Fill in project if it's not defined.
        if (this.pushHook.getProject() == null && this.pushHook.getRepository() != null) {
            try {
                String path = new URL(this.pushHook.getRepository().getGitHttpUrl()).getPath();
                if (StringUtils.isNotBlank(path)) {
                    Project project = new Project();
                    project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                    this.pushHook.setProject(project);
                } else {
                    LOGGER.log(Level.WARNING, "Could not find suitable namespace.");
                }
            } catch (MalformedURLException ignored) {
                LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace.");
            }
        }
    }

    public void execute() {
        if (pushHook.getRepository() != null && pushHook.getRepository().getUrl() == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        if (project instanceof Job<?, ?>) {
            ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                @Override
                protected void performOnPost(GitLabPushTrigger trigger) {
                    trigger.onPost(pushHook);
                }
            });
            throw HttpResponses.ok();
        }
        if (project instanceof SCMSourceOwner) {
            ACL.impersonate(ACL.SYSTEM, new SCMSourceOwnerNotifier());
            throw HttpResponses.ok();
        }
        throw HttpResponses.errorWithoutStack(409, "Push Hook is not supported for this project");
    }

    private class SCMSourceOwnerNotifier implements Runnable {
        public void run() {
            for (SCMSource scmSource : ((SCMSourceOwner) project).getSCMSources()) {
                if (scmSource instanceof GitSCMSource) {
                    GitSCMSource gitSCMSource = (GitSCMSource) scmSource;
                    try {
                        if (new URIish(gitSCMSource.getRemote()).equals(new URIish(gitSCMSource.getRemote()))) {
                            if (!gitSCMSource.isIgnoreOnPushNotifications()) {
                                LOGGER.log(Level.FINE, "Notify scmSourceOwner {0} about changes for {1}",
                                           toArray(project.getName(), gitSCMSource.getRemote()));
                                ((SCMSourceOwner) project).onSCMSourceUpdated(scmSource);
                            } else {
                                LOGGER.log(Level.FINE, "Ignore on push notification for scmSourceOwner {0} about changes for {1}",
                                           toArray(project.getName(), gitSCMSource.getRemote()));
                            }
                        }
                    } catch (URISyntaxException e) {
                        // nothing to do
                    }
                }
            }

            // TODO configuration can be done via "trigger" like the existing GitlabPushTrigger or similar object
            // TODO should be replaced with getClass comparison to prevent the dependency to the branch api
            if (project instanceof MultiBranchProject) {
                // TODO should be extracted
                String targetBranch = pushHook.getRef().replaceFirst("^refs/heads/", "");
                // just for getting a gitlab connection
                Job targetJob = ((MultiBranchProject)project).getItemByBranchName(targetBranch);

                if (targetJob != null) {
                    GitLabConnectionProperty property = ((Job<?, ?>) targetJob).getProperty(GitLabConnectionProperty.class);

                    if (property != null && property.getClient() != null && pushHook.getProjectId() != null) {
                        for (MergeRequest mergeRequest : getOpenMergeRequests(property.getClient(), pushHook.getProjectId())) {
                            // no forks supported yet
                            if (mergeRequest.getTargetProjectId().equals(pushHook.getProjectId())
                                    && mergeRequest.getSourceProjectId().equals(pushHook.getProjectId())
                                    && mergeRequest.getTargetBranch().equals(targetBranch)) {
                                String sourceBranch = mergeRequest.getSourceBranch();
                                Job job = ((MultiBranchProject)project).getItemByBranchName(sourceBranch);

                                if (job != null) {
                                    LOGGER.log(Level.FINE, "Found branch for merge request {0}, triggering {1}",
                                        toArray(mergeRequest.getIid(), sourceBranch));
                                    try {
                                        // use reflection to prevent another dependency to the workflow-job api
                                        // TODO pass action object to get some cause data of the build
                                        Method method = job.getClass().getDeclaredMethod("scheduleBuild");
                                        method.invoke(this);
                                    } catch (NoSuchMethodException e) {
                                        LOGGER.log(Level.FINE, "No scheduleBuild method found for job class {0}", job.getClass());
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        LOGGER.log(Level.FINE, "Execution of scheduleBuild failed", e);
                                    }
                                }
                            }
                        }
                    } else {
                        LOGGER.log(Level.FINE, "No gitlab connection configured or system configuration is wrong for {0} of {1}.",
                            toArray(targetBranch, project.getName()));
                    }
                } else {
                    LOGGER.log(Level.FINE, "Job for target branch {0} does not exist in MultiBranchProject {1}",
                        toArray(targetBranch, project.getName()));
                }
            }
        }
    }

    // TODO should be extracted
    private List<MergeRequest> getOpenMergeRequests(GitLabApi client, Integer projectId) {
        List<MergeRequest> result = new ArrayList<>();
        Integer page = 1;
        do {
            List<MergeRequest> mergeRequests = client.getMergeRequests(projectId.toString(), State.opened, page, 100);
            result.addAll(mergeRequests);
            page = mergeRequests.isEmpty() ? null : page + 1;
        } while (page != null);
        return result;
    }
}
