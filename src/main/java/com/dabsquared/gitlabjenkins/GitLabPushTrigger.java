package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.cause.GitLabMergeCause;
import com.dabsquared.gitlabjenkins.cause.GitLabPushCause;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.model.WebHook;
import com.dabsquared.gitlabjenkins.webhook.GitLabWebHook;
import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.plugins.git.RevisionParameterAction;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.XStream2;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.gitlab.api.GitlabAPI;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.AntPathMatcher;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;


/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<Job<?, ?>> {
	private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());
	private boolean triggerOnPush = true;
    private boolean triggerOnMergeRequest = true;
    private final String triggerOpenMergeRequestOnPush;
    private boolean ciSkip = true;
    private boolean setBuildDescription = true;
    private boolean addNoteOnMergeRequest = true;
    private boolean addCiMessage = false;
    private boolean addVoteOnMergeRequest = true;
    private transient boolean allowAllBranches = false;
    private final String branchFilterName;
    private final String includeBranchesSpec;
    private final String excludeBranchesSpec;
    private final String targetBranchRegex;
    private boolean acceptMergeRequestOnSuccess = false;


    @DataBoundConstructor
    public GitLabPushTrigger(boolean triggerOnPush, boolean triggerOnMergeRequest, String triggerOpenMergeRequestOnPush,
                             boolean ciSkip, boolean setBuildDescription, boolean addNoteOnMergeRequest, boolean addCiMessage,
                             boolean addVoteOnMergeRequest, boolean acceptMergeRequestOnSuccess, String branchFilterName,
                             String includeBranchesSpec, String excludeBranchesSpec, String targetBranchRegex) {
        this.triggerOnPush = triggerOnPush;
        this.triggerOnMergeRequest = triggerOnMergeRequest;
        this.triggerOpenMergeRequestOnPush = triggerOpenMergeRequestOnPush;
        this.ciSkip = ciSkip;
        this.setBuildDescription = setBuildDescription;
        this.addNoteOnMergeRequest = addNoteOnMergeRequest;
        this.addCiMessage = addCiMessage;
        this.addVoteOnMergeRequest = addVoteOnMergeRequest;
        this.branchFilterName = branchFilterName;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.targetBranchRegex = targetBranchRegex;
        this.acceptMergeRequestOnSuccess = acceptMergeRequestOnSuccess;
    }

    public boolean getTriggerOnPush() {
    	return triggerOnPush;
    }

    public boolean getTriggerOnMergeRequest() {
    	return triggerOnMergeRequest;
    }

    public String getTriggerOpenMergeRequestOnPush() {
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

    /**
     * @deprecated see {@link com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher}
     */
    @Deprecated
    public boolean getAddCiMessage() {
        return addCiMessage;
    }

    /**
     * @deprecated see {@link com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher}
     */
    @Deprecated
    public void setAddCiMessage(boolean addCiMessage) {
        this.addCiMessage = addCiMessage;
    }

    public boolean getCiSkip() {
        return ciSkip;
    }

    private boolean isAllowedByTargetBranchRegex(String branchName) {
        final String regex = this.getTargetBranchRegex();

        if (StringUtils.isEmpty(regex)) {
            return true;
        }
        final Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(branchName).matches();
    }

    private boolean isAllowedByList(final String branchName) {

        final List<String> exclude = DescriptorImpl.splitBranchSpec(this.getExcludeBranchesSpec());
        final List<String> include = DescriptorImpl.splitBranchSpec(this.getIncludeBranchesSpec());
        if (exclude.isEmpty() && include.isEmpty()) {
            return true;
        }

        final AntPathMatcher matcher = new AntPathMatcher();
        for (final String pattern : exclude) {
            if (matcher.match(pattern, branchName)) {
                return false;
            }
        }
        for (final String pattern : include) {
            if (matcher.match(pattern, branchName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBranchAllowed(final String branchName) {

        final String branchFilterName = this.getBranchFilterName();
        if (branchFilterName.isEmpty()) {
            // no filter is applied, allow all branches
            return true;
        }

        if (Objects.equal(branchFilterName, "NameBasedFilter")) {
            return this.isAllowedByList(branchName);
        }

        if (Objects.equal(branchFilterName, "RegexBasedFilter")) {
            return this.isAllowedByTargetBranchRegex(branchName);
        }

        return false;
    }

    // TODO use an enum instead of a String for this
    public String getBranchFilterName() {
        // TODO move this to a migration method during code cleanup
        if (branchFilterName == null) {
            return  allowAllBranches ? "" : "NameBasedFilter";
        } else {
            return branchFilterName;
        }
    }

    public String getIncludeBranchesSpec() {
        return this.includeBranchesSpec == null ? "" : this.includeBranchesSpec;
    }

    public String getExcludeBranchesSpec() {
        return this.excludeBranchesSpec == null ? "" : this.excludeBranchesSpec;
    }

    public String getTargetBranchRegex() { return this.targetBranchRegex == null ? "" : this.targetBranchRegex; }

    // executes when the Trigger receives a push request
    public void onPost(final PushHook pushHook) {
        // TODO 1.621+ use standard method
        final ParameterizedJobMixIn scheduledJob = new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };

        if (triggerOnPush && this.isBranchAllowed(pushHook.getRef().replaceFirst("^refs/heads/", ""))) {

            LOGGER.log(Level.INFO, "{0} triggered for push.", job.getFullName());

            Action[] actions = createActions(pushHook, job);

            int projectbuildDelay = 0;

            if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob)job;
                if (abstractProject.getQuietPeriod() > projectbuildDelay) {
                    projectbuildDelay = abstractProject.getQuietPeriod();
                }
            }

            scheduledJob.scheduleBuild2(projectbuildDelay, actions);
        }
    }

    private GitLabPushCause createGitLabPushCause(PushHook pushHook) {
        GitLabPushCause cause;
        try {
            cause = new GitLabPushCause(pushHook, getLogFile());
        } catch (IOException ex) {
            cause = new GitLabPushCause(pushHook);
        }
        return cause;
    }

    private Action[] createActions(PushHook pushHook, Job job) {
        ArrayList<Action> actions = new ArrayList<Action>();
	    actions.add(new CauseAction(createGitLabPushCause(pushHook)));
        RevisionParameterAction revision;
        revision = createPushRequestRevisionParameter(job, pushHook);
        if (revision==null) {
            return null;
        }

        actions.add(revision);
        Action[] actionsArray = actions.toArray(new Action[0]);

        return actionsArray;
    }

    public RevisionParameterAction createPushRequestRevisionParameter(Job<?, ?> job, PushHook pushHook) {
        RevisionParameterAction revision = null;

        URIish urIish = null;
        try {
            urIish = new URIish(pushHook.getRepository().getUrl());
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }

        if (pushHook.getCommits().isEmpty()) {
            if (pushHook.getBefore() != null
                    && pushHook.getBefore().contains("0000000000000000000000000000000000000000")) {
                // new branches
                revision = new RevisionParameterAction(pushHook.getAfter(), urIish);
            } else {
                LOGGER.log(Level.WARNING,
                        "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                        new Object[] {pushHook, (job!=null?job.getFullName():null)});
                return null;
            }
        } else {
            revision = new RevisionParameterAction(pushHook.getCommits().get(0).getId(), urIish);
        }
        return revision;
    }

    // executes when the Trigger receives a merge request
    public void onPost(final MergeRequestHook mergeRequestHook) {
        final ParameterizedJobMixIn scheduledJob = new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };

        if (triggerOnMergeRequest && this.isBranchAllowed(mergeRequestHook.getObjectAttributes().getTargetBranch())) {

    	    LOGGER.log(Level.INFO, "{0} triggered for merge request.", job.getFullName());

	        GitLabMergeCause cause = createGitLabMergeCause(mergeRequestHook);

	        int projectbuildDelay = 0;

	        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob)job;
                if (abstractProject.getQuietPeriod() > projectbuildDelay) {
                    projectbuildDelay = abstractProject.getQuietPeriod();
                }
	        }

	        scheduledJob.scheduleBuild2(projectbuildDelay, new CauseAction(cause));
    	} else {
	        LOGGER.log(Level.INFO, "trigger on merge request not set");
	    }
    }

    private GitLabMergeCause createGitLabMergeCause(MergeRequestHook mergeRequestHook) {
        GitLabMergeCause cause;
        try {
            cause = new GitLabMergeCause(mergeRequestHook, getLogFile());
        } catch (IOException ex) {
            cause = new GitLabMergeCause(mergeRequestHook);
        }
        return cause;
    }

    private void setBuildCauseInJob(Run run){
        if(setBuildDescription){
            Cause pcause= run.getCause(GitLabPushCause.class);
            Cause mcause= run.getCause(GitLabMergeCause.class);
            String desc = null;
            if(pcause!=null) desc = pcause.getShortDescription();
            if(mcause!=null) desc = mcause.getShortDescription();
            if(desc!=null && desc.length()>0){
                try {
                    run.setDescription(desc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.get();
    }

    public static DescriptorImpl getDesc() {
        return DescriptorImpl.get();
    }

    public File getLogFile() {
        return new File(job.getRootDir(), "gitlab-polling.log");
    }

    public static final class ConverterImpl extends XStream2.PassthruConverter<GitLabPushTrigger> {

        public ConverterImpl(final XStream2 xstream) {
            super(xstream);

            xstream.registerLocalConverter(GitLabPushTrigger.class, "includeBranchesSpec", new Converter() {

                public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
                    if ("includeBranchesSpec".equalsIgnoreCase(reader.getNodeName())) {
                        return reader.getValue();
                    }
                    if ("allowedBranchesSpec".equalsIgnoreCase(reader.getNodeName())) {
                        return reader.getValue();
                    }
                    if ("allowedBranches".equalsIgnoreCase(reader.getNodeName())) {
                        final Converter iconv = new CollectionConverter(xstream.getMapper(), List.class);
                        final List<?> list = (List<?>) iconv.unmarshal(reader, context);
                        return Joiner.on(',').join(list);
                    }

                    throw new AbstractReflectionConverter.UnknownFieldException(context.getRequiredType().getName(), reader.getNodeName());
                }

                public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
                    writer.setValue(String.valueOf(source));
                }

                public boolean canConvert(final Class type) {
                    return List.class.isAssignableFrom(type) || String.class.isAssignableFrom(type);
                }
            });

            synchronized (xstream) {
                xstream.setMapper(new MapperWrapper(xstream.getMapperInjectionPoint()) {

                    @Override
                    public String realMember(final Class type, final String serialized) {
                        if (GitLabPushTrigger.class.equals(type)) {
                            if ("allowedBranchesSpec".equalsIgnoreCase(serialized) || "allowedBranches".equalsIgnoreCase(serialized)) {
                                return "includeBranchesSpec";
                            }
                        }
                        return super.realMember(type, serialized);
                    }

                });
            }
        }

        @Override
        protected void callback(final GitLabPushTrigger obj, final UnmarshallingContext context) {
            /* no-op */
        }

    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        Job project;
        private String gitlabApiToken;
        private String gitlabHostUrl = "";
        private boolean ignoreCertificateErrors = false;
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);
        private transient GitLab gitlab;

        public DescriptorImpl() {
        	load();
        }

        @Override
        public boolean isApplicable(Item item) {
            if(item instanceof Job && SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob) {
                project = (Job) item;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getDisplayName() {
            if(project == null) {
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
            save();
            gitlab = new GitLab();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillTriggerOpenMergeRequestOnPushItems(@QueryParameter String triggerOpenMergeRequestOnPush) {
            return new ListBoxModel(new Option("Never", "never", triggerOpenMergeRequestOnPush.matches("never") ),
                    new Option("On push to source branch", "source", triggerOpenMergeRequestOnPush.matches("source") ),
                    new Option("On push to source or target branch", "both", triggerOpenMergeRequestOnPush.matches("both") ));
        }

        private List<String> getProjectBranches(final Job<?, ?> job) throws IOException, IllegalStateException {
            if (!(job instanceof AbstractProject<?, ?>)) {
                return Lists.newArrayList();
            }

            final URIish sourceRepository = getSourceRepoURLDefault(job);

            if (sourceRepository == null) {
                throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
            }

            if (!getGitlabHostUrl().isEmpty()) {
                return GitLabProjectBranchesService.instance().getBranches(getGitlab(), sourceRepository.toString());
            } else {
                LOGGER.log(Level.WARNING, "getProjectBranches: gitlabHostUrl hasn't been configured globally. Job {0}.",
                        job.getFullName());
                return Lists.newArrayList();
            }
        }

        private GitSCM getGitSCM(SCMTriggerItem item) {
            if(item != null) {
                for(SCM scm : item.getSCMs()) {
                    if(scm instanceof GitSCM) {
                        return (GitSCM) scm;
                    }
                }
            }
            return null;
        }

        private static List<String> splitBranchSpec(final String spec) {
            return Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(spec));
        }

        private AutoCompletionCandidates doAutoCompleteBranchesSpec(final Job<?, ?> job, @QueryParameter final String value) {
            String query = value.toLowerCase();

            final AutoCompletionCandidates ac = new AutoCompletionCandidates();
            List<String> values = ac.getValues();

            try {
                List<String> branches = this.getProjectBranches(job);
                // show all suggestions for short strings
                if (query.length() < 2){
                    values.addAll(branches);
                } else {
                    for (String branch : branches){
                      if (branch.toLowerCase().indexOf(query) > -1){
                        values.add(branch);
                      }
                    }
                }
            } catch (final IllegalStateException ex) {
                LOGGER.log(Level.FINEST, "Unexpected IllegalStateException. Please check the logs and your configuration.", ex);
            } catch (final IOException ex) {
                LOGGER.log(Level.FINEST, "Unexpected IllegalStateException. Please check the logs and your configuration.", ex);
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
                final String unknownBranchNames = StringUtils.join(branchSpecs, ", ");
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
        protected URIish getSourceRepoURLDefault(Job job) {
            URIish url = null;
            SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(job);
            GitSCM gitSCM = getGitSCM(item);
            if(gitSCM == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not find GitSCM for project. Project = {1}, next build = {2}",
                        new String[] {
                                project.getName(),
                                String.valueOf(project.getNextBuildNumber()) });
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
        protected String getSourceRepoNameDefault(Job job) {
            String result = null;
            SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(job);
            GitSCM gitSCM = getGitSCM(item);
            if(gitSCM == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not find GitSCM for project. Project = {1}, next build = {2}",
                        new String[] {
                                project.getName(),
                                String.valueOf(project.getNextBuildNumber()) });
                throw new IllegalArgumentException("This project does not use git:" + project.getName());
            } else {
                List<RemoteConfig> repositories = gitSCM.getRepositories();
                if (!repositories.isEmpty()){
                    result = repositories.get(repositories.size()-1).getName();
                }
            }
            return result;
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

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
