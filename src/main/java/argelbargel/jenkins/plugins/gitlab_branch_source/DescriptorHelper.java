package argelbargel.jenkins.plugins.gitlab_branch_source;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabAPIException;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSourceOwner;
import org.gitlab.api.models.GitlabProject;
import org.jenkinsci.plugins.gitclient.GitClient;

import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabAPI;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabConnection;
import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabHelper.gitLabConnectionNames;
import static argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectSelector.VISIBLE;
import static argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabProjectVisibility.ALL;

class DescriptorHelper {
    static final String CHECKOUT_CREDENTIALS_ANONYMOUS = "ANONYMOUS";
    private static final Logger LOGGER = Logger.getLogger(DescriptorHelper.class.getName());


    static FormValidation doCheckConnectionName(String connectionName) {
        return gitLabConnectionNames().contains(connectionName) ? FormValidation.ok() : FormValidation.error(connectionName + " is not a valid GitLab Connection");
    }

    static FormValidation doCheckIncludes(String includes) {
        if (includes.isEmpty()) {
            return FormValidation.warning(Messages.GitLabSCMSource_did_you_mean_to_use_to_match_all_branches());
        }
        return FormValidation.ok();
    }

    static ListBoxModel doFillConnectionNameItems() {
        ListBoxModel items = new ListBoxModel();
        for (String name : gitLabConnectionNames()) {
            items.add(name, name);
        }
        return items;
    }

    static ListBoxModel doFillProjectPathItems(String connectionName) {
        StandardListBoxModel result = new StandardListBoxModel();

        try {
            for (GitlabProject project : gitLabAPI(connectionName).findProjects(VISIBLE, ALL, "")) {
                result.add(project.getPathWithNamespace(), project.getPathWithNamespace());
            }
        } catch (GitLabAPIException e) {
            LOGGER.warning("could not find any projects for connection " + connectionName + ": " + e.getMessage());
        }

        return result;
    }

    static ListBoxModel doFillCheckoutCredentialsIdItems(SCMSourceOwner context, String connectionName, String value) {
        if (context == null && !Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER) ||
                context != null && !context.hasPermission(Item.EXTENDED_READ)) {
            return new StandardListBoxModel().includeCurrentValue(value);
        }

        StandardListBoxModel result = new StandardListBoxModel();
        result.includeEmptyValue();
        result.add("- anonymous -", CHECKOUT_CREDENTIALS_ANONYMOUS);
        return result.includeMatchingAs(
                context instanceof Queue.Task
                        ? Tasks.getDefaultAuthenticationOf((Queue.Task) context)
                        : ACL.SYSTEM,
                context,
                StandardUsernameCredentials.class,
                gitLabConnectionRequirements(connectionName),
                GitClient.CREDENTIALS_MATCHER
        );
    }

    private static List<DomainRequirement> gitLabConnectionRequirements(String connectioName) {
        URIRequirementBuilder builder = URIRequirementBuilder.create();

        try {
            URL connectionURL = new URL(gitLabConnection(connectioName).getUrl());
            builder.withHostnamePort(connectionURL.getHost(), connectionURL.getPort());
        } catch (Exception ignored) {
            LOGGER.fine("ignoring invalid gitlab-connection: " + connectioName);
        }

        return builder.build();
    }


    private DescriptorHelper() { /* NO INSTANCES ALLOWED */ }
}
