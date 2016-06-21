package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.connection.GitLabConnection;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.NoteHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import com.dabsquared.gitlabjenkins.trigger.branch.ProjectBranchesProvider;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandler;
import com.dabsquared.gitlabjenkins.trigger.handler.note.NoteHookTriggerHandler;
import com.dabsquared.gitlabjenkins.trigger.handler.push.PushHookTriggerHandler;
import com.dabsquared.gitlabjenkins.webhook.GitLabWebHook;
import com.sun.org.apache.xpath.internal.operations.Bool;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.regex.Pattern;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory.newMergeRequestHookTriggerHandler;
import static com.dabsquared.gitlabjenkins.trigger.handler.note.NoteHookTriggerHandlerFactory.newNoteHookTriggerHandler;
import static com.dabsquared.gitlabjenkins.trigger.handler.push.PushHookTriggerHandlerFactory.newPushHookTriggerHandler;


/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<Job<?, ?>> {
	private boolean triggerOnPush = true;
    private boolean triggerOnMergeRequest = true;
    private final TriggerOpenMergeRequest triggerOpenMergeRequestOnPush;
    private boolean triggerOnNoteRequest = true;
    private final String noteRegex;
    private boolean ciSkip = true;
    private boolean setBuildDescription = true;
    private boolean addNoteOnMergeRequest = true;
    private boolean addCiMessage = false;
    private boolean addVoteOnMergeRequest = true;
    private transient boolean allowAllBranches = false;
    private transient BranchFilterType branchFilterName;
    private BranchFilterType branchFilterType;
    private String includeBranchesSpec;
    private String excludeBranchesSpec;
    private String targetBranchRegex;
    private transient BranchFilter branchFilter;
    private transient PushHookTriggerHandler pushHookTriggerHandler;
    private transient MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler;
    private transient NoteHookTriggerHandler noteHookTriggerHandler;
    private boolean acceptMergeRequestOnSuccess = false;


    @DataBoundConstructor
    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public GitLabPushTrigger(boolean triggerOnPush, boolean triggerOnMergeRequest, TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
                             boolean triggerOnNoteRequest, String noteRegex, boolean ciSkip, boolean setBuildDescription,
                             boolean addNoteOnMergeRequest, boolean addCiMessage, boolean addVoteOnMergeRequest,
                             boolean acceptMergeRequestOnSuccess, BranchFilterType branchFilterType,
                             String includeBranchesSpec, String excludeBranchesSpec, String targetBranchRegex) {
        this.triggerOnPush = triggerOnPush;
        this.triggerOnMergeRequest = triggerOnMergeRequest;
        this.triggerOnNoteRequest = triggerOnNoteRequest;
        this.noteRegex = noteRegex;
        this.triggerOpenMergeRequestOnPush = triggerOpenMergeRequestOnPush;
        this.ciSkip = ciSkip;
        this.setBuildDescription = setBuildDescription;
        this.addNoteOnMergeRequest = addNoteOnMergeRequest;
        this.addCiMessage = addCiMessage;
        this.addVoteOnMergeRequest = addVoteOnMergeRequest;
        this.branchFilterType = branchFilterType;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.targetBranchRegex = targetBranchRegex;
        this.acceptMergeRequestOnSuccess = acceptMergeRequestOnSuccess;

        initializeTriggerHandler();
        initializeBranchFilter();
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void migrateJobs() throws IOException {
        GitLabPushTrigger.DescriptorImpl oldConfig = Trigger.all().get(GitLabPushTrigger.DescriptorImpl.class);
        if (!oldConfig.jobsMigrated) {
            GitLabConnectionConfig gitLabConfig = (GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class);
            gitLabConfig.getConnections().add(new GitLabConnection(oldConfig.gitlabHostUrl,
                    oldConfig.gitlabHostUrl,
                    oldConfig.gitlabApiToken,
                    oldConfig.ignoreCertificateErrors,
                    10,
                    10));

            String defaultConnectionName = gitLabConfig.getConnections().get(0).getName();
            for (AbstractProject<?, ?> project : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                if (trigger != null) {
                    if (trigger.addCiMessage) {
                        project.getPublishersList().add(new GitLabCommitStatusPublisher());
                    }
                    if (trigger.branchFilterType == null) {
                        trigger.branchFilterType = trigger.branchFilterName;
                    }
                    project.addProperty(new GitLabConnectionProperty(defaultConnectionName));
                    project.save();
                }
            }
            gitLabConfig.save();
            oldConfig.jobsMigrated = true;
            oldConfig.save();
        }
    }

    public boolean getTriggerOnPush() {
        return triggerOnPush;
    }

    public boolean getTriggerOnMergeRequest() {
        return triggerOnMergeRequest;
    }

    public boolean getTriggerOnNoteRequest() {
        return triggerOnNoteRequest;
    }

    public String getNoteRegex() {
        return this.noteRegex == null ? "" : this.noteRegex;
    }

    public TriggerOpenMergeRequest getTriggerOpenMergeRequestOnPush() {
        return triggerOpenMergeRequestOnPush;
    }

    public boolean getSetBuildDescription() {
        return setBuildDescription;
    }

    public boolean getAddNoteOnMergeRequest() {
        return addNoteOnMergeRequest;
    }

    public boolean getAddVoteOnMergeRequest() {
        return addVoteOnMergeRequest;
    }

    public boolean getAcceptMergeRequestOnSuccess() {
        return acceptMergeRequestOnSuccess;
    }

    public boolean getCiSkip() {
        return ciSkip;
    }

    public BranchFilterType getBranchFilterType() {
        return branchFilterType;
    }

    public String getIncludeBranchesSpec() {
        return includeBranchesSpec;
    }

    public String getExcludeBranchesSpec() {
        return excludeBranchesSpec;
    }

    public String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    // executes when the Trigger receives a push request
    public void onPost(final PushHook hook) {
        pushHookTriggerHandler.handle(job, hook, ciSkip, branchFilter);
    }

    // executes when the Trigger receives a merge request
    public void onPost(final MergeRequestHook hook) {
        mergeRequestHookTriggerHandler.handle(job, hook, ciSkip, branchFilter);
    }

    // executes when the Trigger receives a note request
    public void onPost(final NoteHook hook) {
        noteHookTriggerHandler.handle(job, hook, ciSkip, branchFilter);
    }

    private void initializeTriggerHandler() {
        mergeRequestHookTriggerHandler = newMergeRequestHookTriggerHandler(triggerOnMergeRequest, triggerOpenMergeRequestOnPush);
        noteHookTriggerHandler = newNoteHookTriggerHandler(triggerOnNoteRequest, noteRegex);
        pushHookTriggerHandler = newPushHookTriggerHandler(triggerOnPush, triggerOpenMergeRequestOnPush);
    }

    private void initializeBranchFilter() {
        branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec(includeBranchesSpec)
                .withExcludeBranchesSpec(excludeBranchesSpec)
                .withTargetBranchRegex(targetBranchRegex)
                .build(branchFilterType));
    }

    @Override
    protected Object readResolve() throws ObjectStreamException {
        initializeTriggerHandler();
        initializeBranchFilter();
        return super.readResolve();
    }

    public static GitLabPushTrigger getFromJob(Job<?, ?> job) {
        GitLabPushTrigger trigger = null;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob p = (ParameterizedJobMixIn.ParameterizedJob) job;
            for (Trigger t : p.getTriggers().values()) {
                if (t instanceof GitLabPushTrigger) {
                    trigger = (GitLabPushTrigger) t;
                }
            }
        }
        return trigger;
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);
        private boolean jobsMigrated = false;
        private String gitlabApiToken;
        private String gitlabHostUrl = "";
        private boolean ignoreCertificateErrors = false;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job
                    && SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            Job<?, ?> project = retrieveCurrentJob();
            if (project != null) {
                try {
                    return "Build when a change is pushed to GitLab. GitLab CI Service URL: " + retrieveProjectUrl(project);
                } catch (IllegalStateException e) {
                    // nothing to do
                }
            }
            return "Build when a change is pushed to GitLab, unknown URL";
        }

        private StringBuilder retrieveProjectUrl(Job<?, ?> project) {
            return new StringBuilder()
                    .append(Jenkins.getInstance().getRootUrl())
                    .append(GitLabWebHook.WEBHOOK_URL)
                    .append(retrieveParentUrl(project))
                    .append('/').append(Util.rawEncode(project.getName()));
        }

        private StringBuilder retrieveParentUrl(Item item) {
            if (item.getParent() instanceof Item) {
                Item parent = (Item) item.getParent();
                return retrieveParentUrl(parent).append('/').append(Util.rawEncode(parent.getName()));
            } else {
                return new StringBuilder();
            }
        }

        private Job<?, ?> retrieveCurrentJob() {
            StaplerRequest request = Stapler.getCurrentRequest();
            if (request != null) {
                Ancestor ancestor = request.findAncestor(Job.class);
                return ancestor == null ? null : (Job<?, ?>) ancestor.getObject();
            }
            return null;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillTriggerOpenMergeRequestOnPushItems(@QueryParameter String triggerOpenMergeRequestOnPush) {
            return new ListBoxModel(new Option("Never", "never", triggerOpenMergeRequestOnPush.matches("never")),
                    new Option("On push to source branch", "source", triggerOpenMergeRequestOnPush.matches("source")),
                    new Option("On push to source or target branch", "both", triggerOpenMergeRequestOnPush.matches("both")));
        }

        public AutoCompletionCandidates doAutoCompleteIncludeBranchesSpec(@AncestorInPath final Job<?, ?> job, @QueryParameter final String value) {
            return ProjectBranchesProvider.instance().doAutoCompleteBranchesSpec(job, value);
        }

        public AutoCompletionCandidates doAutoCompleteExcludeBranchesSpec(@AncestorInPath final Job<?, ?> job, @QueryParameter final String value) {
            return ProjectBranchesProvider.instance().doAutoCompleteBranchesSpec(job, value);
        }

        public FormValidation doCheckIncludeBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
            return ProjectBranchesProvider.instance().doCheckBranchesSpec(project, value);
        }

        public FormValidation doCheckExcludeBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
            return ProjectBranchesProvider.instance().doCheckBranchesSpec(project, value);
        }
    }
}
