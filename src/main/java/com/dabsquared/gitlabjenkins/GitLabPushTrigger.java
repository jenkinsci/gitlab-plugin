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
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<AbstractProject<?, ?>> {
	private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());
	private boolean triggerOnPush = true;
    private boolean triggerOnMergeRequest = true;
    private List<String> allowedBranches;

	@DataBoundConstructor
    public GitLabPushTrigger(boolean triggerOnPush, boolean triggerOnMergeRequest, List<String> allowedBranches) {
        this.triggerOnPush = triggerOnPush;
        this.triggerOnMergeRequest = triggerOnMergeRequest;
        if (allowedBranches.isEmpty())
        	this.allowedBranches = getDescriptor().getProjectBranches();
        else 
        	this.allowedBranches = allowedBranches;
    }

    public boolean getTriggerOnPush() {
    	return triggerOnPush;
    }

    public boolean getTriggerOnMergeRequest() {
    	return triggerOnMergeRequest;
    }
    
    public List<String> getAllowedBranches() {
    	return allowedBranches;
    }

    public void onPost(final GitLabPushRequest req) {
    	if (triggerOnPush && (allowedBranches.contains("*") || allowedBranches.contains(getSourceBranch(req)))) {
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

                    String branch = getSourceBranch(req);

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
    }

    public void onPost(final GitLabMergeRequest req) {
    	if (triggerOnMergeRequest) {
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
                    values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", getSourceBranch(req)));
                    values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", req.getObjectAttribute().getTargetBranch()));
                
                    String sourceRepoName = "origin";
                    String sourceRepoURL = getDesc().getSourceRepoURLDefault().toString();
                    
                    if (!getDescriptor().getGitlabHostUrl().isEmpty()) {                                        
                    	// Get source repository if communication to Gitlab is possible
                    	try {
                        	sourceRepoName = req.getSourceProject(getDesc().getGitlab()).getPathWithNamespace();    
                        	sourceRepoURL = req.getSourceProject(getDesc().getGitlab()).getSshUrl();    
                        } catch (IOException ex) {
                        	LOGGER.log(Level.WARNING, "Could not fetch source project''s data from Gitlab. '('{0}':' {1}')'", new String[]{ex.toString(), ex.getMessage()});                        	
                        }
                    }
                    
                    values.put("gitlabSourceRepoName", new StringParameterValue("gitlabSourceRepoName", sourceRepoName));
                	values.put("gitlabSourceRepoURL", new StringParameterValue("gitlabSourceRepoURL", sourceRepoURL));
                	                    
                    List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

                    ParametersAction parametersAction = new ParametersAction(listValues);
                    actions.add(parametersAction);

                    Action[] actionsArray = actions.toArray(new Action[0]);

                    return actionsArray;
                }
                
                
            });	
    	}
    }
    
    private String getSourceBranch(GitLabRequest req) {
    	String result = null;
    	if (req instanceof GitLabPushRequest) {
    		result = ((GitLabPushRequest)req).getRef().replaceAll("refs/heads/", "");
    	} else {
    		result = ((GitLabMergeRequest)req).getObjectAttribute().getSourceBranch();
    	}
    	
    	return result;
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

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        AbstractProject project;
        private String gitlabApiToken;
        private String gitlabHostUrl = "";
        private boolean ignoreCertificateErrors = false;
        private List<String> projectBranches = null;
        
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
            save();
            gitlab = new GitLab();
            return super.configure(req, formData);
        }       
        
        public List<String> getProjectBranches() {        	
        	projectBranches = new ArrayList<String>();
        	try {
            	/* TODO until java-gitlab-api v1.1.5 is released,
            		cannot search projects by namespace/name
            		For now getting project id before getting project branches
            	 */
        		URIish sourceRepository = getSourceRepoURLDefault();
        		if (gitlabHostUrl.isEmpty() || null == sourceRepository) {
        			projectBranches.add("*");
        		} else {
        			List<GitlabProject> projects = getGitlab().instance().getProjects();
        			for (GitlabProject project : projects) {
						if(project.getSshUrl().equalsIgnoreCase(sourceRepository.toString())){
							//Get all branches of project
							List<GitlabBranch> branches = getGitlab().instance().getBranches(project);
							for (GitlabBranch branch : branches){
								projectBranches.add(branch.getName());
							}
							break;
						}					
					}    			
        		}				
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Could not fetch source project''s data from Gitlab. '('{0}':' {1}')'", new String[]{ex.toString(), ex.getMessage()});
				projectBranches.add("*");
			}
        	LOGGER.log(Level.FINE, "Fetched {0} Repository branches", projectBranches.size());
        	return projectBranches;
        }
        
        /**
         * Get the URL of the first declared repository in the project configuration.
         * Use this as default source repository url.
         * 
         * @return URIish the default value of the source repository url
         */
        protected URIish getSourceRepoURLDefault() {
        	URIish url = null;
        	SCM scm = project.getScm();
        	if(!(scm instanceof GitSCM)) {
                throw new IllegalArgumentException("This repo does not use git.");
            }
            if (scm instanceof GitSCM) {
            	List<RemoteConfig> repositories = ((GitSCM) scm).getRepositories();
            	if (!repositories.isEmpty()){
            		RemoteConfig defaultRepository = repositories.get(repositories.size()-1);
                	List<URIish> uris = defaultRepository.getURIs();
                	if (!uris.isEmpty()) {
                		url = uris.get(uris.size()-1);
                	}                    	
            	}           
            } 
        	return url;
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
                return FormValidation.error("Client error : "+e.getMessage());
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

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
