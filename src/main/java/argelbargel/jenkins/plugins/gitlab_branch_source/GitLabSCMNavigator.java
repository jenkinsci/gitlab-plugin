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
import java.util.List;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.DescriptorHelper.CHECKOUT_CREDENTIALS_ANONYMOUS;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabConnectionId;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.ICON_GITLAB_LOGO;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMIcons.iconfilePathPattern;


@SuppressWarnings({"unused", "WeakerAccess" })
public class GitLabSCMNavigator extends SCMNavigator {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMNavigator.class.getName());
    private static final String DEFAULT_SEARCH_PATTERN = "";

    private final SourceSettings sourceSettings;
    private final GitLabSCMWebHookListener hookListener;
    private String projectSearchPattern;
    private String projectSelectorId;
    private String projectVisibilityId;

    @DataBoundConstructor
    public GitLabSCMNavigator(String connectionName, String checkoutCredentialsId) {
        this.sourceSettings = new SourceSettings(connectionName, checkoutCredentialsId);
        this.hookListener = GitLabSCMWebHook.createListener(this);
        this.projectSearchPattern = DEFAULT_SEARCH_PATTERN;
        this.projectSelectorId = GitLabProjectSelector.VISIBLE.id();
        this.projectVisibilityId = GitLabProjectVisibility.ALL.id();
    }

    SourceSettings getSourceSettings() {
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
    public void setMonitorBranches(boolean monitorBranches) {
        sourceSettings.branchMonitorStrategy().setMonitored(monitorBranches);
        if (!monitorBranches) {
            sourceSettings.setBuildBranchesWithMergeRequests(false);
        }
    }

    public boolean getMonitorAndBuildBranches() {
        return sourceSettings.branchMonitorStrategy().monitored();
    }

    @DataBoundSetter
    public void setBuildBranchesWithMergeRequests(boolean value) {
        sourceSettings.setBuildBranchesWithMergeRequests(sourceSettings.branchMonitorStrategy().monitored() && value);
    }

    public boolean getBuildBranchesWithMergeRequests() {
        return sourceSettings.originMonitorStrategy().monitored() && sourceSettings.getBuildBranchesWithMergeRequests();
    }

    @DataBoundSetter
    public void setMonitorAndBuildMergeRequestsFromOrigin(boolean value) {
        sourceSettings.originMonitorStrategy().setMonitored(value);
    }

    public boolean getMonitorAndBuildMergeRequestsFromOrigin() {
        return sourceSettings.originMonitorStrategy().monitored();
    }

    @DataBoundSetter
    public void setBranchBuildStatusPublishMode(String value) {
        sourceSettings.branchMonitorStrategy().setBuildStatusPublishMode(BuildStatusPublishMode.valueOf(value));
    }

    public String getBranchBuildStatusPublishMode() {
        return sourceSettings.branchMonitorStrategy().getBuildStatusPublishMode().name();
    }

    @DataBoundSetter
    public void setBuildMergeRequestsFromOriginMerged(boolean value) {
        sourceSettings.originMonitorStrategy().setBuildMerged(value);
    }

    public boolean getBuildMergeRequestsFromOriginMerged() {
        return sourceSettings.originMonitorStrategy().buildMerged();
    }

    @DataBoundSetter
    public void setOriginBuildStatusPublishMode(String value) {
        sourceSettings.originMonitorStrategy().setBuildStatusPublishMode(BuildStatusPublishMode.valueOf(value));
    }

    public String getOriginBuildStatusPublishMode() {
        return sourceSettings.originMonitorStrategy().getBuildStatusPublishMode().name();
    }

    @DataBoundSetter
    public void setBuildOnlyMergeableRequestsFromOriginMerged(boolean value) {
        sourceSettings.originMonitorStrategy().setBuildOnlyMergeableRequestsMerged(value);
    }

    public boolean getBuildOnlyMergeableRequestsFromOriginMerged() {
        return sourceSettings.originMonitorStrategy().buildOnlyMergeableRequestsMerged();
    }

    @DataBoundSetter
    public void setBuildMergeRequestsFromOriginUnmerged(boolean value) {
        sourceSettings.originMonitorStrategy().setBuildUnmerged(value);
    }

    public boolean getBuildMergeRequestsFromOriginUnmerged() {
        return sourceSettings.originMonitorStrategy().buildUnmerged();
    }

    @DataBoundSetter
    public void setIgnoreWorkInProgressFromOrigin(boolean ignoreWIPMergeRequests) {
        sourceSettings.originMonitorStrategy().setIgnoreWorkInProgress(ignoreWIPMergeRequests);
    }

    public boolean getIgnoreWorkInProgressFromOrigin() {
        return sourceSettings.originMonitorStrategy().ignoreWorkInProgress();
    }

    @DataBoundSetter
    public void setAcceptMergeRequestsFromOrigin(boolean value) {
        sourceSettings.originMonitorStrategy().setAcceptMergeRequests(value);
    }

    public boolean getAcceptMergeRequestsFromOrigin() {
        return sourceSettings.originMonitorStrategy().getAcceptMergeRequests();
    }

    @DataBoundSetter
    public void setRemoveSourceBranchFromOrigin(boolean value) {
        sourceSettings.originMonitorStrategy().setRemoveSourceBranch(value);
    }

    public boolean getRemoveSourceBranchFromOrigin() {
        return sourceSettings.originMonitorStrategy().getRemoveSourceBranch();
    }


    @DataBoundSetter
    public void setMonitorAndBuildMergeRequestsFromForks(boolean value) {
        sourceSettings.forksMonitorStrategy().setMonitored(value);
    }

    public boolean getMonitorAndBuildMergeRequestsFromForks() {
        return sourceSettings.forksMonitorStrategy().monitored();
    }

    @DataBoundSetter
    public void setForkBuildStatusPublishMode(String value) {
        sourceSettings.forksMonitorStrategy().setBuildStatusPublishMode(BuildStatusPublishMode.valueOf(value));
    }

    public String getForkBuildStatusPublishMode() {
        return sourceSettings.forksMonitorStrategy().getBuildStatusPublishMode().name();
    }

    @DataBoundSetter
    public void setBuildMergeRequestsFromForksMerged(boolean value) {
        sourceSettings.forksMonitorStrategy().setBuildMerged(value);
    }

    public boolean getBuildMergeRequestsFromForksMerged() {
        return sourceSettings.forksMonitorStrategy().buildMerged();
    }

    @DataBoundSetter
    public void setAcceptMergeRequestsFromForks(boolean value) {
        sourceSettings.forksMonitorStrategy().setAcceptMergeRequests(value);
    }

    public boolean getAcceptMergeRequestsFromForks() {
        return sourceSettings.forksMonitorStrategy().getAcceptMergeRequests();
    }

    @DataBoundSetter
    public void setBuildOnlyMergeableRequestsFromForksMerged(boolean value) {
        sourceSettings.forksMonitorStrategy().setBuildOnlyMergeableRequestsMerged(value);
    }

    public boolean getBuildOnlyMergeableRequestsFromForksMerged() {
        return sourceSettings.forksMonitorStrategy().buildOnlyMergeableRequestsMerged();
    }

    @DataBoundSetter
    public void setBuildMergeRequestsFromForksUnmerged(boolean value) {
        sourceSettings.forksMonitorStrategy().setBuildUnmerged(value);
    }

    public boolean getBuildMergeRequestsFromForksUnmerged() {
        return sourceSettings.forksMonitorStrategy().buildUnmerged();
    }

    @DataBoundSetter
    public void setIgnoreWorkInProgressFromForks(boolean ignoreWIPMergeRequests) {
        sourceSettings.forksMonitorStrategy().setIgnoreWorkInProgress(ignoreWIPMergeRequests);
    }

    public boolean getIgnoreWorkInProgressFromForks() {
        return sourceSettings.forksMonitorStrategy().ignoreWorkInProgress();
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
    public void setMonitorTags(boolean monitorTags) {
        sourceSettings.tagMonitorStrategy().setMonitored(monitorTags);
    }

    public boolean getMonitorTags() {
        return sourceSettings.tagMonitorStrategy().monitored();
    }

    @DataBoundSetter
    public void setBuildTags(boolean buildTags) {
        sourceSettings.tagMonitorStrategy().setBuildUnmerged(buildTags);
    }

    public boolean getBuildTags() {
        return sourceSettings.tagMonitorStrategy().buildUnmerged();
    }

    @DataBoundSetter
    public void setTagBuildStatusPublishMode(String value) {
        sourceSettings.tagMonitorStrategy().setBuildStatusPublishMode(BuildStatusPublishMode.valueOf(value));
    }

    public String getTagBuildStatusPublishMode() {
        return sourceSettings.tagMonitorStrategy().getBuildStatusPublishMode().name();
    }

    @DataBoundSetter
    public void setRegisterWebHooks(boolean registerWebHooks) {
        // invert value as GUI passes false if box is checked because of negative=true in optionalBlock
        sourceSettings.setRegisterWebHooks(!registerWebHooks);
    }


    public boolean getRegisterWebHooks() {
        return sourceSettings.getRegisterWebHooks();
    }

    public String getPublisherName() {
        return sourceSettings.getPublisherName();
    }

    @DataBoundSetter
    public void setPublisherName(String name) {
        sourceSettings.setPublisherName(name);
    }

    @DataBoundSetter
    public void setPublishUnstableBuildsAsSuccess(boolean value) {
        sourceSettings.setPublishUnstableBuildsAsSuccess(value);
    }

    public boolean getPublishUnstableBuildsAsSuccess() {
        return sourceSettings.getPublishUnstableBuildsAsSuccess();
    }

    @DataBoundSetter
    public void setMergeCommitMessage(String value) {
        sourceSettings.setMergeCommitMessage(value);
    }

    public String getMergeCommitMessage() {
        return sourceSettings.getMergeCommitMessage();
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMNavigatorOwner owner, @CheckForNull SCMNavigatorEvent event, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        return Collections.<Action>singletonList(GitLabLinkAction.toServer(getPronoun(), getConnectionName()));
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

    private SourceVisitor createVisitor(@Nonnull SCMSourceObserver observer) {
        return new SourceVisitor(this, observer);
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
        public String getIconClassName() {
            return ICON_GITLAB_LOGO;
        }

        @Override
        public String getIconFilePathPattern() {
            return iconfilePathPattern(getIconClassName());
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
        public ListBoxModel doFillBranchBuildStatusPublishModeItems() {
            return DescriptorHelper.doBuildStatusPublishModeItems();
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillOriginBuildStatusPublishModeItems() {
            return DescriptorHelper.doBuildStatusPublishModeItems();
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillForkBuildStatusPublishModeItems() {
            return DescriptorHelper.doBuildStatusPublishModeItems();
        }

        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillTagBuildStatusPublishModeItems() {
            return DescriptorHelper.doBuildStatusPublishModeItems();
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
