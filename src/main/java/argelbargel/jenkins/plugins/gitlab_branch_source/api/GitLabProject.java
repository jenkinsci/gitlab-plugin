package argelbargel.jenkins.plugins.gitlab_branch_source.api;


import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.ACL;
import jenkins.plugins.git.AbstractGitSCMSource;
import org.gitlab.api.models.GitlabProject;

import javax.annotation.Nonnull;
import java.util.Collections;


public class GitLabProject extends GitlabProject {
    public String getRemote(AbstractGitSCMSource source) {
        if (source.getCredentialsId() != null && credentials(source, StandardCredentials.class) instanceof SSHUserPrivateKey) {
            return getSshUrl();
        } else {
            return getHttpUrl();
        }
    }

    private <T extends StandardCredentials> T credentials(AbstractGitSCMSource source, @Nonnull Class<T> type) {
        return CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(
                type, source.getOwner(), ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList()), CredentialsMatchers.allOf(
                CredentialsMatchers.withId(source.getCredentialsId()),
                CredentialsMatchers.instanceOf(type)));
    }
}
