package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabMergeRequest;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.filters.GitLabMergeRequestFilter;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.hooks.GitLabSCMWebHook;
import argelbargel.jenkins.plugins.gitlab_branch_source.hooks.GitLabSCMWebHookListener;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.browser.GitLab;
import hudson.plugins.git.browser.GitRepositoryBrowser;
import hudson.scm.SCM;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.api.metadata.ObjectMetadataAction;
import org.eclipse.jgit.transport.RefSpec;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabTag;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GitLabSCMSource extends AbstractGitSCMSource {
    private static final RefSpec REFSPEC_BRANCHES = new RefSpec("+refs/heads/*:refs/remotes/origin/*");
    private static final RefSpec REFSPEC_TAGS = new RefSpec("+refs/tags/*:refs/remotes/origin/tags/*");
    private static final RefSpec REFSPEC_MERGE_REQUESTS = new RefSpec("+refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*");
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMSource.class.getName());

    private final GitLabSCMSourceSettings settings;
    private final GitlabProject project;
    private final GitLabSCMWebHookListener hookListener;

    GitLabSCMSource(GitlabProject project, GitLabSCMSourceSettings settings) {
        super(project.getPathWithNamespace());
        this.settings = settings;
        this.project = project;
        this.hookListener = GitLabSCMWebHook.createListener(this);
    }

    public String getConnectionName() {
        return settings.getConnectionName();
    }

    @Override
    public String getCredentialsId() {
        return settings.getCredentialsId();
    }

    @Override
    public String getRemote() {
        if (getCredentialsId() != null && getCredentials(StandardCredentials.class) instanceof SSHUserPrivateKey) {
            return project.getSshUrl();
        } else {
            return project.getHttpUrl();
        }
    }

    @Override
    public String getIncludes() {
        return settings.getIncludes();
    }

    @Override
    public String getExcludes() {
        return settings.getExcludes();
    }

    public boolean getBuildBranches() {
        return settings.getBuildBranches();
    }

    public boolean getBuildBranchesWithMergeRequests() {
        return settings.getBuildBranchesWithMergeRequests();
    }

    public boolean getBuildMergeRequestsFromOrigin() {
        return settings.getBuildMergeRequestsFromOrigin();
    }

    public Set<GitLabSCMMergeRequestBuildStrategy> getOriginMergeRequestBuildStrategies() {
        return settings.getOriginMergeRequestBuildStrategies();
    }

    public boolean getIgnoreOriginWIPMergeRequests() {
        return settings.getIgnoreOriginWIPMergeRequests();
    }

    public boolean getBuildMergeRequestsFromForks() {
        return settings.getBuildMergeRequestsFromForks();
    }

    public Set<GitLabSCMMergeRequestBuildStrategy> getForkMergeRequestBuildStrategies() {
        return settings.getForkMergeRequestBuildStrategies();
    }

    public boolean getBuildTags() {
        return settings.getBuildTags();
    }

    public boolean getIgnoreForkWIPMergeRequests() {
        return settings.getIgnoreForkWIPMergeRequests();
    }

    public boolean getRegisterWebHooks() {
        return settings.getRegisterWebHooks();
    }

    public boolean getUpdateBuildDescription() {
        return settings.getUpdateBuildDescription();
    }

    public int getProjectId() {
        return project.getId();
    }

    public String getHookUrl() {
        return hookListener.url().toString();
    }

    public GitLabSCMWebHookListener getHookListener() {
        return hookListener;
    }

    @Override
    public GitRepositoryBrowser getBrowser() {
        try {
            return new GitLab(project.getWebUrl(), gitLabAPI(getConnectionName()).getVersion().toString());
        } catch (GitLabAPIException e) {
            LOGGER.warning("could not determine gitlab-version:" + e.getMessage());
            return super.getBrowser();
        }
    }

    @Override
    protected void retrieve(@CheckForNull SCMSourceCriteria criteria, @Nonnull SCMHeadObserver observer, @CheckForNull SCMHeadEvent<?> event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
//        Set<String> originBranchesWithMergeRequest = new HashSet<>();

        if (settings.getBuildMergeRequests()) {
            GitLabMergeRequestFilter filter = settings.getMergeRequestFilter();
            for (GitLabMergeRequest mr : filter.filter(gitLabAPI(getConnectionName()).getMergeRequests(getProjectId()))) {
                checkInterrupt();

                GitLabSCMMergeRequest head = new GitLabSCMMergeRequest(mr);
                observer.observe(new GitLabSCMHolder(head, head.getSource()), head.getCommit());
//                originBranchesWithMergeRequest.add(head.getTarget().getName());
            }
        }

        if (settings.getBuildBranches()) {
            for (GitlabBranch branch : gitLabAPI(getConnectionName()).getBranches(getProjectId())) {
                checkInterrupt();

                GitLabSCMBranch head = new GitLabSCMBranch(branch);
//                if (settings.getBuildBranchesWithMergeRequests() || !originBranchesWithMergeRequest.contains(head.getName())) {
                    observer.observe(new GitLabSCMHolder(head), head.getCommit());
//                }
            }
        }

        if (settings.getBuildTags()) {
            for (GitlabTag tag : gitLabAPI(getConnectionName()).getTags(getProjectId())) {
                checkInterrupt();

                GitLabSCMTag head = new GitLabSCMTag(tag);
                observer.observe(new GitLabSCMHolder(head), head.getCommit());
            }
        }
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@CheckForNull SCMSourceEvent event, @Nonnull TaskListener listener) throws IOException {
        return Arrays.asList(
                new ObjectMetadataAction(null, project.getDescription(), project.getWebUrl()),
                new ObjectMetadataAction(Messages.GitLabSCMSource_DefaultBranch(), project.getDefaultBranch(), GitLabLink.treeUrl(project, project.getDefaultBranch())),
                GitLabLink.toProject(project),
                GitLabLink.toTree(project, project.getDefaultBranch()));

    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMHead head, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (head instanceof GitLabSCMHolder) {
            return retrieveActions(((GitLabSCMHolder) head).getTarget(), event, listener);
        }

        List<Action> actions = new ArrayList<>();
        actions.add(GitLabLink.toTree(project, head.getName()));
        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause(), getUpdateBuildDescription()));
        }

        return actions;
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        if (revision.getHead() instanceof GitLabSCMHolder) {
            return retrieveActions(((GitLabSCMHolder) revision.getHead()).getTarget(), event, listener);
        }

        List<Action> actions = new ArrayList<>();
        if (event instanceof GitLabSCMEvent) {
            actions.add(new GitLabSCMCauseAction(((GitLabSCMEvent) event).getCause(), getUpdateBuildDescription()));
        }

        if (revision instanceof GitLabSCMCommit) {
            String hash = ((GitLabSCMCommit) revision).getHash();
            actions.add(GitLabLink.toCommit(project, hash));
            actions.add(GitLabLink.toTree(project, hash));
        } else {
            actions.add(GitLabLink.toTree(project, revision.getHead().getName()));
        }

        return actions;
    }

    @Override
    protected List<RefSpec> getRefSpecs() {
        List<RefSpec> refSpecs = new LinkedList<>();
        if (settings.getBuildBranches()) {
            refSpecs.add(REFSPEC_BRANCHES);
        }

        if (settings.getBuildTags()) {
            refSpecs.add(REFSPEC_TAGS);
        }

        if (settings.getBuildMergeRequests()) {
            refSpecs.add(REFSPEC_MERGE_REQUESTS);
        }
        return refSpecs;
    }


    @Override
    public void afterSave() {
        GitLabSCMWebHook.get().addListener(this);
    }

    @Nonnull
    @Override
    public SCM build(@Nonnull SCMHead head, @CheckForNull SCMRevision revision) {
        GitSCM scm = (GitSCM) super.build((head instanceof GitLabSCMHolder) ? ((GitLabSCMHolder) head).getTarget() : head, revision);
        scm.setBrowser(getBrowser());
        return scm;
    }

    private <T extends StandardCredentials> T getCredentials(@Nonnull Class<T> type) {
        return CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(
                type, getOwner(), ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.allOf(
                CredentialsMatchers.withId(getCredentialsId()),
                CredentialsMatchers.instanceOf(type)));
    }

    @SuppressWarnings("unused")
    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {
        @Nonnull
        public String getDisplayName() {
            return Messages.GitLabSCMSource_DisplayName();
        }

        @Override
        public String getPronoun() {
            return Messages.GitLabSCMSource_Pronoun();
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckConnectionName(@AncestorInPath SCMSourceOwner context, @QueryParameter String connectionName) {
            return DescriptorHelper.doCheckConnectionName(connectionName);
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckIncludes(@QueryParameter String includes) {
            return DescriptorHelper.doCheckIncludes(includes);
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillConnectionNameItems() {
            return DescriptorHelper.doFillConnectionNameItems();
        }

        public ListBoxModel doFillProjectPathItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String connectionName) {
            return DescriptorHelper.doFillProjectPathItems(connectionName);
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String connectionName, @QueryParameter String checkoutCredentialsId) {
            return DescriptorHelper.doFillCheckoutCredentialsIdItems(context, connectionName, checkoutCredentialsId);
        }
    }
}
