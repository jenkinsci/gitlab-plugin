package argelbargel.jenkins.plugins.gitlab_branch_source.hooks;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;


@SuppressWarnings("unused")
@Extension
public final class GitLabSCMWebHookItemListener extends ItemListener {
    @Override
    public void onDeleted(Item item) {
        if (item instanceof SCMNavigatorOwner) {
            onDeleted((SCMNavigatorOwner) item);
        }

        if (item instanceof SCMSourceOwner) {
            onDeleted((SCMSourceOwner) item);
        }
    }

    private void onDeleted(SCMNavigatorOwner owner) {
        for (SCMNavigator navigator : owner.getSCMNavigators()) {
            if (navigator instanceof GitLabSCMNavigator) {
                if (!StringUtils.isEmpty(((GitLabSCMNavigator) navigator).getConnectionName())) {
                    GitLabSCMWebHook.get().removeListener((GitLabSCMNavigator) navigator);
                }
            }
        }
    }

    private void onDeleted(SCMSourceOwner owner) {
        for (SCMSource source : owner.getSCMSources()) {
            if (source instanceof GitLabSCMSource) {
                GitLabSCMWebHook.get().removeListener((GitLabSCMSource) source);
            }
        }
    }
}
