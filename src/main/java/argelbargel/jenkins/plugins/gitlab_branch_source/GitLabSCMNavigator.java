package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectSelector;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectVisibility;
import argelbargel.jenkins.plugins.gitlab_branch_source.hooks.GitLabSCMWebHook;
import argelbargel.jenkins.plugins.gitlab_branch_source.hooks.GitLabSCMWebHookListener;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorDescriptor;
import jenkins.scm.api.SCMNavigatorEvent;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSourceCategory;
import jenkins.scm.api.SCMSourceObserver;
import jenkins.scm.api.SCMSourceOwner;
import jenkins.scm.impl.UncategorizedSCMSourceCategory;
import org.apache.commons.codec.digest.DigestUtils;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.DescriptorHelper.CHECKOUT_CREDENTIALS_ANONYMOUS;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabConnectionId;


@SuppressWarnings({"unused", "WeakerAccess" })
public class GitLabSCMNavigator extends SCMNavigator {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMNavigator.class.getName());
    private static final String DEFAULT_SEARCH_PATTERN = "";

    private final GitLabSCMSourceSettings sourceSettings;
    private final GitLabSCMWebHookListener hookListener;
    private String projectSearchPattern;
    private String projectSelectorId;
    private String projectVisibilityId;

    @DataBoundConstructor
    public GitLabSCMNavigator(String connectionName, String checkoutCredentialsId) {
        this.sourceSettings = new GitLabSCMSourceSettings(connectionName, checkoutCredentialsId);
        this.hookListener = GitLabSCMWebHook.createListener(this);
        this.projectSearchPattern = DEFAULT_SEARCH_PATTERN;
        this.projectSelectorId = GitLabProjectSelector.VISIBLE.id();
        this.projectVisibilityId = GitLabProjectVisibility.ALL.id();
    }

    GitLabSCMSourceSettings getSourceSettings() {
        return sourceSettings;
    }

    @CheckForNull
    public String getConnectionName() {
        return sourceSettings.getConnectionName();
    }

    @CheckForNull
    public String getCheckoutCredentialsId() {
        return sourceSettings.getCredentialsId();
    }

    public String getProjectSearchPattern() {
        return projectSearchPattern;
    }

    @DataBoundSetter
    public void setProjectSearchPattern(String pattern) {
        projectSearchPattern = (pattern != null) ? pattern : DEFAULT_SEARCH_PATTERN;
    }

    public String getProjectSelectorId() {
        return projectSelectorId;
    }

    @DataBoundSetter
    public void setProjectSelectorId(String id) {
        projectSelectorId = id;
    }

    public String getProjectVisibilityId() {
        return projectVisibilityId;
    }

    @DataBoundSetter
    public void setProjectVisibilityId(String id) {
        projectVisibilityId = id;
    }

    @Nonnull
    public String getIncludes() {
        return sourceSettings.getIncludes();
    }

    @DataBoundSetter
    public void setIncludes(@Nonnull String includes) {
        sourceSettings.setIncludes(includes);
    }

    @Nonnull
    public String getExcludes() {
        return sourceSettings.getExcludes();
    }

    @DataBoundSetter
    public void setExcludes(@Nonnull String excludes) {
        sourceSettings.setExcludes(excludes);
    }

    @DataBoundSetter
    public void setBuildBranches(boolean buildBranches) {
        sourceSettings.setBuildBranches(buildBranches);
        if (!buildBranches) {
            sourceSettings.setBuildBranchesWithMergeRequests(false);
        }
    }

    public boolean getBuildBranches() {
        return sourceSettings.getBuildBranches();
    }

    @DataBoundSetter
    public void setBuildBranchesWithMergeRequests(boolean value) {
        sourceSettings.setBuildBranchesWithMergeRequests(sourceSettings.getBuildBranches() && value);
    }

    public boolean getBuildBranchesWithMergeRequests() {
        return sourceSettings.getBuildBranchesWithMergeRequests();
    }

    @DataBoundSetter
    public void setBuildMergeRequestsFromOrigin(boolean value) {
        if (!value) {
            sourceSettings.setOriginMergeRequestBuildStrategies(Collections.<GitLabSCMMergeRequestBuildStrategy>emptySet());
        }
    }

    public boolean getBuildMergeRequestsFromOrigin() {
        return sourceSettings.getBuildMergeRequestsFromOrigin();
    }

    @DataBoundSetter
    public void setOriginMergeRequestBuildStrategies(Set<GitLabSCMMergeRequestBuildStrategy> value) {
        if (getBuildMergeRequestsFromOrigin()) {
            sourceSettings.setOriginMergeRequestBuildStrategies(value);
        } else {
            sourceSettings.setOriginMergeRequestBuildStrategies(Collections.<GitLabSCMMergeRequestBuildStrategy>emptySet());
            sourceSettings.setIgnoreOriginWIPMergeRequests(true);
        }
    }

    public Set<GitLabSCMMergeRequestBuildStrategy> getOriginMergeRequestBuildStrategies() {
        return (getBuildMergeRequestsFromOrigin()) ? sourceSettings.getOriginMergeRequestBuildStrategies() : EnumSet.of(GitLabSCMMergeRequestBuildStrategy.MERGED);
    }

    @DataBoundSetter
    public void setBuildMergeRequestsFromForks(boolean value) {
        if (!value) {
            sourceSettings.setForkMergeRequestBuildStrategies(Collections.<GitLabSCMMergeRequestBuildStrategy>emptySet());
        }
    }

    public boolean getBuildMergeRequestsFromForks() {
        return sourceSettings.getBuildMergeRequestsFromForks();
    }

    @DataBoundSetter
    public void setForkMergeRequestBuildStrategies(Set<GitLabSCMMergeRequestBuildStrategy> value) {
        if (getBuildMergeRequestsFromForks()) {
            sourceSettings.setForkMergeRequestBuildStrategies(value);
        } else {
            sourceSettings.setForkMergeRequestBuildStrategies(Collections.<GitLabSCMMergeRequestBuildStrategy>emptySet());
            sourceSettings.setIgnoreForkWIPMergeRequests(true);
        }
    }

    public Set<GitLabSCMMergeRequestBuildStrategy> getForkMergeRequestBuildStrategies() {
        return (getBuildMergeRequestsFromOrigin()) ? sourceSettings.getForkMergeRequestBuildStrategies() : EnumSet.of(GitLabSCMMergeRequestBuildStrategy.MERGED);
    }

    @DataBoundSetter
    public void setUpdateBuildDescription(boolean updateBuildDescription) {
        sourceSettings.setUpdateBuildDescription(updateBuildDescription);
    }

    public boolean getUpdateBuildDescription() {
        return sourceSettings.getUpdateBuildDescription();
    }


    public String getHookUrl() {
        return hookListener.url().toString();
    }

    @DataBoundSetter
    public void setBuildTags(boolean buildTags) {
        sourceSettings.setBuildTags(buildTags);
    }

    public boolean getBuildTags() {
        return sourceSettings.getBuildTags();
    }

    @DataBoundSetter
    public void setIgnoreOriginWIPMergeRequests(boolean ignoreWIPMergeRequests) {
        sourceSettings.setIgnoreOriginWIPMergeRequests(ignoreWIPMergeRequests);
    }

    public boolean getIgnoreOriginWIPMergeRequests() {
        return sourceSettings.getIgnoreOriginWIPMergeRequests();
    }

    @DataBoundSetter
    public void setIgnoreForkWIPMergeRequests(boolean ignoreWIPMergeRequests) {
        sourceSettings.setIgnoreForkWIPMergeRequests(ignoreWIPMergeRequests);
    }

    public boolean getIgnoreForkWIPMergeRequests() {
        return sourceSettings.getIgnoreForkWIPMergeRequests();
    }

    @DataBoundSetter
    public void setRegisterWebHooks(boolean registerWebHooks) {
        // invert value as GUI passes false if box is checked because of negative=true in optionalBlock
        sourceSettings.setRegisterWebHooks(!registerWebHooks);
    }

    public boolean getRegisterWebHooks() {
        return sourceSettings.getRegisterWebHooks();
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMNavigatorOwner owner, @CheckForNull SCMNavigatorEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        return Collections.<Action>singletonList(GitLabLink.toServer(getConnectionName()));
    }

    @Override
    public void visitSources(@Nonnull SCMSourceObserver observer) throws IOException, InterruptedException {
        LOGGER.info("visiting sources for context " + observer.getContext().getFullName() + "...");
        createVisitor(observer).visitSources();
    }

    @Override
    public void visitSource(@Nonnull String sourceName, @Nonnull SCMSourceObserver observer) throws IOException, InterruptedException {
        LOGGER.info("visiting " + sourceName + " for context " + observer.getContext().getFullName());
        createVisitor(observer).visitProject(sourceName);
    }

    @Override
    public void afterSave(@Nonnull SCMNavigatorOwner owner) {
        LOGGER.info("auto-registering system-hook for " + owner.getFullName() + "...");
        GitLabSCMWebHook.get().addListener(this);
    }

    private GitLabSCMSourceVisitor createVisitor(@Nonnull SCMSourceObserver observer) {
        return new GitLabSCMSourceVisitor(this, observer);
    }

    @Nonnull
    @Override
    protected String id() {
        return gitLabConnectionId(getConnectionName()) + "::" + DigestUtils.md5Hex(getProjectSelectorId() + getProjectVisibilityId() + getProjectSearchPattern());
    }

    public GitLabSCMWebHookListener getHookListener() {
        return hookListener;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    @Extension
    public static class Descriptor extends SCMNavigatorDescriptor implements IconSpec {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.GitLabSCMNavigator_DisplayName();
        }

        @Override
        public String getPronoun() {
            return Messages.GitLabSCMNavigator_Pronoun();
        }

        @Nonnull
        @Override
        public String getDescription() {
            return Messages.GitLabSCMNavigator_Description();
        }

        @Override
        public SCMNavigator newInstance(String name) {
            return new GitLabSCMNavigator("", CHECKOUT_CREDENTIALS_ANONYMOUS);
        }

        @Nonnull
        @Override
        protected SCMSourceCategory[] createCategories() {
            return new SCMSourceCategory[]{
                    new UncategorizedSCMSourceCategory(Messages._GitLabSCMNavigator_UncategorizedCategory())
            };
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

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillCheckoutCredentialsIdItems(@AncestorInPath SCMSourceOwner context, @QueryParameter String connectionName, @QueryParameter String checkoutCredentialsId) {
            return DescriptorHelper.doFillCheckoutCredentialsIdItems(context, connectionName, checkoutCredentialsId);
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckProjectSelectorId(@AncestorInPath SCMSourceOwner context, @QueryParameter String projectSelectorId) {
            return GitLabProjectSelector.ids().contains(projectSelectorId) ? FormValidation.ok() : FormValidation.error("invalid selector-id: " + projectSelectorId);
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckProjectVisibilityId(@AncestorInPath SCMSourceOwner context, @QueryParameter String projectVisibilityId) {
            return GitLabProjectVisibility.ids().contains(projectVisibilityId) ? FormValidation.ok() : FormValidation.error("invalid visibility-id: " + projectVisibilityId);
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillProjectSelectorIdItems() {
            ListBoxModel items = new ListBoxModel();
            for (String id : GitLabProjectSelector.ids()) {
                items.add(id, id);
            }
            return items;
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillProjectVisibilityIdItems() {
            ListBoxModel items = new ListBoxModel();
            for (String id : GitLabProjectVisibility.ids()) {
                items.add(id, id);
            }
            return items;
        }
    }
}
