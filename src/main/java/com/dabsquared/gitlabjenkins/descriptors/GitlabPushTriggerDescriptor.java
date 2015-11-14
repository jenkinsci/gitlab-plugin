package com.dabsquared.gitlabjenkins.descriptors;

import com.dabsquared.gitlabjenkins.GitLab;
import com.dabsquared.gitlabjenkins.GitLabWebHook;
import com.dabsquared.gitlabjenkins.Messages;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import net.sf.json.JSONObject;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class GitlabPushTriggerDescriptor extends TriggerDescriptor {

    private static final Logger LOGGER = Logger.getLogger(GitlabPushTriggerDescriptor.class.getName());
    public static final String DEFAULT_BUILD_MSG = "${icon} Jenkins Build ${buildResult}\n\nResults are available at: [Jenkins](${buildUrl})";
    public static final String DEFAULT_MERGE_REQUEST_ACCEPT_MSG = "Merge Request accepted by Jenkins build success";
    private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);
    private final Map<String, List<String>> projectBranches = new HashMap<String, List<String>>();
    Job project;
    private String gitlabApiToken;
    private String gitlabHostUrl = "";

    private String buildSuccessMsg = DEFAULT_BUILD_MSG;
    private String buildFailureMsg = DEFAULT_BUILD_MSG;
    private String mergeRequestAcceptMsg = DEFAULT_BUILD_MSG;
    private String buildAbortedMsg = DEFAULT_BUILD_MSG;
    private String buildUnstableMsg = DEFAULT_BUILD_MSG;

    private boolean ignoreCertificateErrors = false;
    private transient GitLab gitlab;

    public GitlabPushTriggerDescriptor() {
        load();
    }

    public static List<String> splitBranchSpec(final String spec) {
        return Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(spec));
    }

    @Override
    public boolean isApplicable(Item item) {
        if (item instanceof Job && SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item) != null
            && item instanceof ParameterizedJobMixIn.ParameterizedJob) {
            project = (Job) item;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getDisplayName() {
        if (project == null) {
            return "Build when a change is pushed to GitLab, unknown URL";
        }

        final List<String> projectParentsUrl = new ArrayList<String>();

        try {
            for (Object parent = project.getParent(); parent instanceof Item; parent = ((Item) parent)
                .getParent()) {
                projectParentsUrl.add(0, ((Item) parent).getName());
            }
        } catch (IllegalStateException e) {
            return "Build when a change is pushed to GitLab, unknown URL";
        }
        final StringBuilder projectUrl = new StringBuilder();
        projectUrl.append(Jenkins.getInstance().getRootUrl());
        projectUrl.append(GitLabWebHook.WEBHOOK_URL);
        projectUrl.append('/');
        for (final String parentUrl : projectParentsUrl) {
            projectUrl.append(Util.rawEncode(parentUrl));
            projectUrl.append('/');
        }
        projectUrl.append(Util.rawEncode(project.getName()));

        return "Build when a change is pushed to GitLab. GitLab CI Service URL: " + projectUrl;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        gitlabApiToken = formData.getString("gitlabApiToken");
        gitlabHostUrl = formData.getString("gitlabHostUrl");
        ignoreCertificateErrors = formData.getBoolean("ignoreCertificateErrors");

        buildSuccessMsg = formData.getString("buildSuccessMsg");
        buildFailureMsg = formData.getString("buildFailureMsg");
        buildUnstableMsg = formData.getString("buildUnstableMsg");
        buildAbortedMsg = formData.getString("buildAbortedMsg");
        mergeRequestAcceptMsg = formData.getString("mergeRequestAcceptMsg");

        save();
        gitlab = new GitLab();
        return super.configure(req, formData);
    }

    public FormValidation doCheckBuildSuccessMsg(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Empty build success message is not allowed");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckBuildFailureMsg(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Empty build failure message is not allowed");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckBuildAbortedMsg(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Empty build aborted message is not allowed");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckBuildUnstableMsg(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Empty build unstable message is not allowed");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckMergeRequestAcceptMsg(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Empty build success message is not allowed");
        }
        return FormValidation.ok();
    }

    public ListBoxModel doFillTriggerOpenMergeRequestOnPushItems(@QueryParameter String triggerOpenMergeRequestOnPush) {
        return new ListBoxModel(new ListBoxModel.Option("Never", "never", triggerOpenMergeRequestOnPush.matches("never")),
            new ListBoxModel.Option("On push to source branch", "source", triggerOpenMergeRequestOnPush.matches("source")),
            new ListBoxModel.Option("On push to source or target branch", "both", triggerOpenMergeRequestOnPush.matches("both")));
    }

    private List<String> getProjectBranches(final Job<?, ?> job) throws IOException, IllegalStateException {
        if (projectBranches.containsKey(job.getName())) {
            return projectBranches.get(job.getName());
        }

        if (!(job instanceof AbstractProject<?, ?>)) {
            return Lists.newArrayList();
        }

        final URIish sourceRepository = getSourceRepoURLDefault(job);

        if (sourceRepository == null) {
            throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
        }

        try {
            final List<String> branchNames = new ArrayList<String>();
            if (!gitlabHostUrl.isEmpty()) {
                /* TODO until java-gitlab-api v1.1.5 is released,
                 * cannot search projects by namespace/name
                 * For now getting project id before getting project branches */
                final List<GitlabProject> projects = getGitlab().instance().getProjects();
                for (final GitlabProject gitlabProject : projects) {
                    if (gitlabProject.getSshUrl().equalsIgnoreCase(sourceRepository.toString())
                        || gitlabProject.getHttpUrl().equalsIgnoreCase(sourceRepository.toString())) {
                        //Get all branches of project
                        final List<GitlabBranch> branches = getGitlab().instance().getBranches(gitlabProject);
                        for (final GitlabBranch branch : branches) {
                            branchNames.add(branch.getName());
                        }
                        break;
                    }
                }
            }

            projectBranches.put(job.getName(), branchNames);
            return branchNames;
        } catch (final Error error) {
            /* WTF WTF WTF */
            final Throwable cause = error.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw error;
            }
        }
    }

    private GitSCM getGitSCM(SCMTriggerItem item) {
        if (item != null) {
            for (SCM scm : item.getSCMs()) {
                if (scm instanceof GitSCM) {
                    return (GitSCM) scm;
                }
            }
        }
        return null;
    }

    private AutoCompletionCandidates doAutoCompleteBranchesSpec(final Job<?, ?> job, @QueryParameter final String value) {
        String query = value.toLowerCase();

        final AutoCompletionCandidates ac = new AutoCompletionCandidates();
        List<String> values = ac.getValues();

        try {
            List<String> branches = this.getProjectBranches(job);
            // show all suggestions for short strings
            if (query.length() < 2) {
                values.addAll(branches);
            } else {
                for (String branch : branches) {
                    if (branch.toLowerCase().contains(query)) {
                        values.add(branch);
                    }
                }
            }
        } catch (final IllegalStateException ex) {
            /* no-op */
        } catch (final IOException ex) {
            /* no-op */
        }

        return ac;
    }

    public AutoCompletionCandidates doAutoCompleteIncludeBranchesSpec(@AncestorInPath final Job<?, ?> job, @QueryParameter final String value) {
        return this.doAutoCompleteBranchesSpec(job, value);
    }

    public AutoCompletionCandidates doAutoCompleteExcludeBranchesSpec(@AncestorInPath final Job<?, ?> job, @QueryParameter final String value) {
        return this.doAutoCompleteBranchesSpec(job, value);
    }

    private FormValidation doCheckBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
        if (!project.hasPermission(Item.CONFIGURE)) {
            return FormValidation.ok();
        }

        final List<String> branchSpecs = splitBranchSpec(value);
        if (branchSpecs.isEmpty()) {
            return FormValidation.ok();
        }

        final List<String> projectBranches;
        try {
            projectBranches = this.getProjectBranches(project);
        } catch (final IllegalStateException ex) {
            return FormValidation.warning(Messages.GitLabPushTrigger_CannotConnectToGitLab(ex.getMessage()));
        } catch (final IOException ex) {
            return FormValidation.warning(project.hasPermission(Jenkins.ADMINISTER) ? ex : null,
                Messages.GitLabPushTrigger_CannotCheckBranches());
        }

        final Multimap<String, String> matchedSpecs = HashMultimap.create();
        final AntPathMatcher matcher = new AntPathMatcher();
        for (final String projectBranch : projectBranches) {
            for (final String branchSpec : branchSpecs) {
                if (matcher.match(branchSpec, projectBranch)) {
                    matchedSpecs.put(branchSpec, projectBranch);
                }
            }
        }

        branchSpecs.removeAll(matchedSpecs.keySet());
        if (!branchSpecs.isEmpty()) {
            final String unknownBranchNames = org.apache.commons.lang.StringUtils.join(branchSpecs, ", ");
            return FormValidation.warning(Messages.GitLabPushTrigger_BranchesNotFound(unknownBranchNames));
        } else {
            final int matchedBranchesCount = Sets.newHashSet(matchedSpecs.values()).size();
            return FormValidation.ok(Messages.GitLabPushTrigger_BranchesMatched(matchedBranchesCount));
        }
    }

    public FormValidation doCheckIncludeBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
        return this.doCheckBranchesSpec(project, value);
    }

    public FormValidation doCheckExcludeBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
        return this.doCheckBranchesSpec(project, value);
    }

    /**
     * Get the URL of the first declared repository in the project configuration.
     * Use this as default source repository url.
     *
     * @return URIish the default value of the source repository url
     * @throws IllegalStateException Project does not use git scm.
     */
    public URIish getSourceRepoURLDefault(Job job) {
        URIish url = null;
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
        GitSCM gitSCM = getGitSCM(item);
        if (gitSCM == null) {
            LOGGER.log(
                Level.WARNING,
                "Could not find GitSCM for project. Project = {1}, next build = {2}",
                new String[]{
                    project.getName(),
                    String.valueOf(project.getNextBuildNumber())});
            throw new IllegalStateException("This project does not use git:" + project.getName());
        }

        List<RemoteConfig> repositories = gitSCM.getRepositories();
        if (!repositories.isEmpty()) {
            RemoteConfig defaultRepository = repositories.get(repositories.size() - 1);
            List<URIish> uris = defaultRepository.getURIs();
            if (!uris.isEmpty()) {
                return uris.get(uris.size() - 1);
            }
        }

        return null;
    }

    /**
     * Get the Name of the first declared repository in the project configuration.
     * Use this as default source repository Name.
     *
     * @return String with the default name of the source repository
     */
    public String getSourceRepoNameDefault(Job job) {
        String result = null;
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
        GitSCM gitSCM = getGitSCM(item);
        if (gitSCM == null) {
            LOGGER.log(
                Level.WARNING,
                "Could not find GitSCM for project. Project = {1}, next build = {2}",
                new String[]{
                    project.getName(),
                    String.valueOf(project.getNextBuildNumber())});
            throw new IllegalArgumentException("This project does not use git:" + project.getName());
        } else {
            List<RemoteConfig> repositories = gitSCM.getRepositories();
            if (!repositories.isEmpty()) {
                result = repositories.get(repositories.size() - 1).getName();
            }
        }
        return result;
    }

    public FormValidation doCheckGitlabHostUrl(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Gitlab host URL required.");
        }

        return FormValidation.ok();
    }

    public FormValidation doCheckGitlabApiToken(@QueryParameter String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("API Token for Gitlab access required");
        }

        return FormValidation.ok();
    }

    public FormValidation doTestConnection(@QueryParameter("gitlabHostUrl") final String hostUrl,
                                           @QueryParameter("gitlabApiToken") final String token, @QueryParameter("ignoreCertificateErrors") final boolean ignoreCertificateErrors) throws IOException {
        try {
            GitLab.checkConnection(token, hostUrl, ignoreCertificateErrors);
            return FormValidation.ok("Success");
        } catch (IOException e) {
            return FormValidation.error("Client error : " + e.getMessage());
        }
    }

    public GitLab getGitlab() {
        if (gitlab == null) {
            gitlab = new GitLab();
        }
        return gitlab;
    }

    public String getGitlabApiToken() {
        return gitlabApiToken;
    }

    public String getGitlabHostUrl() {
        return gitlabHostUrl;
    }

    public boolean getIgnoreCertificateErrors() {
        return ignoreCertificateErrors;
    }

    public SequentialExecutionQueue getQueue() {
        return queue;
    }

    public String getBuildSuccessMsg() {
        if (buildSuccessMsg == null) {
            buildSuccessMsg = DEFAULT_BUILD_MSG;
        }
        return buildSuccessMsg;
    }

    public String getBuildAbortedMsg() {
        if (buildAbortedMsg == null) {
            buildAbortedMsg = DEFAULT_BUILD_MSG;
        }
        return buildAbortedMsg;
    }

    public String getBuildUnstableMsg() {
        if (buildUnstableMsg == null) {
            buildUnstableMsg = DEFAULT_BUILD_MSG;
        }
        return buildUnstableMsg;
    }

    public String getBuildFailureMsg() {
        if (buildFailureMsg == null) {
            buildFailureMsg = DEFAULT_BUILD_MSG;
        }
        return buildFailureMsg;
    }

    public String getMergeRequestAcceptMsg() {
        if (mergeRequestAcceptMsg == null) {
            mergeRequestAcceptMsg = DEFAULT_MERGE_REQUEST_ACCEPT_MSG;
        }
        return mergeRequestAcceptMsg;
    }
}
