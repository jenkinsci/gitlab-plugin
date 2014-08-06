package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.git.RevisionParameterAction;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<AbstractProject<?, ?>> {


    @DataBoundConstructor
    public GitLabPushTrigger() {

    }

    public void onPost(final GitLabPushRequest req) {
        getDescriptor().queue.execute(new Runnable() {

            public void run() {
                LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                String name = " #" + job.getNextBuildNumber();
                GitLabPushCause cause = createGitLabPushCause(req);
                Action[] actions = createActions(req);

                if (job.scheduleBuild(job.getQuietPeriod(), cause, actions)) {
                    LOGGER.log(Level.INFO, "GitLab Push Request detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                } else {
                    LOGGER.log(Level.INFO, "GitLab Push Request detected in {0}. Job is already in the queue.", job.getName());
                }
            }

            private GitLabPushCause createGitLabPushCause(GitLabPushRequest req) {
                GitLabPushCause cause;
                String triggeredByUser = req.getCommits().get(0).getAuthor().getName();
                try {
                    cause = new GitLabPushCause(triggeredByUser, getLogFile());
                } catch (IOException ex) {
                    cause = new GitLabPushCause(triggeredByUser);
                }
                return cause;
            }

            private Action[] createActions(GitLabPushRequest req) {
                ArrayList<Action> actions = new ArrayList<Action>();

                String branch = req.getRef().replaceAll("refs/heads/", "");

                LOGGER.log(Level.INFO, "GitLab Push Request from branch {0}.", branch);

                Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
                values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", branch));
                values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", branch));
                values.put("gitlabBranch", new StringParameterValue("gitlabBranch", branch));

                List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

                ParametersAction parametersAction = new ParametersAction(listValues);
                actions.add(parametersAction);

                RevisionParameterAction revision = new RevisionParameterAction(req.getLastCommit().getId());
                actions.add(revision);

                Action[] actionsArray = actions.toArray(new Action[0]);

                return actionsArray;
            }

        });
    }

    public void onPost(final GitLabMergeRequest req) {
        getDescriptor().queue.execute(new Runnable() {
            public void run() {
                LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                String name = " #" + job.getNextBuildNumber();
                GitLabMergeCause cause = createGitLabMergeCause(req);
                Action[] actions = createActions(req);

                if (job.scheduleBuild(job.getQuietPeriod(), cause, actions)) {
                    LOGGER.log(Level.INFO, "GitLab Merge Request detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                } else {
                    LOGGER.log(Level.INFO, "GitLab Merge Request detected in {0}. Job is already in the queue.", job.getName());
                }
            }

            private GitLabMergeCause createGitLabMergeCause(GitLabMergeRequest req) {
                GitLabMergeCause cause;
                try {
                    cause = new GitLabMergeCause(req, getLogFile());
                } catch (IOException ex) {
                    cause = new GitLabMergeCause(req);
                }
                return cause;
            }

            private Action[] createActions(GitLabMergeRequest req) {
                List<Action> actions = new ArrayList<Action>();

                Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
                values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", String.valueOf(req.getObjectAttribute().getSourceBranch())));
                values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", String.valueOf(req.getObjectAttribute().getTargetBranch())));

                // Get source repository if communication to Gitlab is possible
                String sourceRepoName = "origin";
                String sourceRepoURL = null;
                
                try {
                	sourceRepoName = req.getSourceProject(getDesc().getGitlab()).getPathWithNamespace();    
                	sourceRepoURL = req.getSourceProject(getDesc().getGitlab()).getSshUrl();    
                } catch (IOException ex) {
                	LOGGER.log(Level.WARNING, "Could not fetch source project''s data from Gitlab. '('{0}':' {1}')'", new String[]{ex.toString(), ex.getMessage()});
                	sourceRepoURL = getSourceRepoURLDefault();
                } finally {
                	values.put("gitlabSourceRepoName", new StringParameterValue("gitlabSourceRepoName", sourceRepoName));
                	values.put("gitlabSourceRepoURL", new StringParameterValue("gitlabSourceRepoURL", sourceRepoURL));
                }
                
                List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

                ParametersAction parametersAction = new ParametersAction(listValues);
                actions.add(parametersAction);

                Action[] actionsArray = actions.toArray(new Action[0]);

                return actionsArray;
            }
            
            /**
             * Get the URL of the first declared repository in the project configuration.
             * Use this as default source repository url.
             * 
             * @return String the default value of the source repository url
             */
            private String getSourceRepoURLDefault() {
            	String url = null;
            	SCM scm = job.getScm();
                if (scm instanceof GitSCM) {
                	List<RemoteConfig> repositories = ((GitSCM) scm).getRepositories();
                	if (!repositories.isEmpty()){
                		RemoteConfig defaultRepository = repositories.get(repositories.size()-1);
                    	List<URIish> uris = defaultRepository.getURIs();
                    	if (!uris.isEmpty()) {
                    		URIish defaultUri = uris.get(uris.size());
                    		url = defaultUri.toString();
                    	}                    	
                	}           
                } 
            	return url;
            }

        });
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

    private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        AbstractProject project;
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
            if(item instanceof AbstractProject) {
                project = (AbstractProject) item;
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
            for (Object parent = project.getParent(); parent instanceof Item; parent = ((Item) parent).getParent()) {
                projectParentsUrl.add(0, ((Item) parent).getName());
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
            save();
            gitlab = new GitLab();
            return super.configure(req, formData);
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

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-jenkins/help/help-trigger.jelly";
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
