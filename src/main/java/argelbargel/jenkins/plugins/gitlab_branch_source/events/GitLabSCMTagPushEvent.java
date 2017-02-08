package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;

import javax.annotation.Nonnull;
import java.util.Collection;

import static argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead.createTag;
import static java.util.Collections.singletonList;
import static jenkins.scm.api.SCMEvent.Type.CREATED;
import static jenkins.scm.api.SCMEvent.Type.REMOVED;


public final class GitLabSCMTagPushEvent extends GitLabSCMPushEvent implements GitLabSCMEvent {
    private static final String NONE_HASH_PATTERN = "^0+$";

    public GitLabSCMTagPushEvent(String id, PushHook hook) {
        super((hook.getBefore().matches(NONE_HASH_PATTERN) ? CREATED : REMOVED), id, hook);
    }

    @Override
    protected boolean isMatch(@Nonnull GitLabSCMSource source) {
        return super.isMatch(source) && source.getMonitorTags();
    }

    @Override
    Collection<GitLabSCMHead> heads(@Nonnull GitLabSCMSource source) {
        PushHook hook = getPayload();
        String hash = getType() == REMOVED ? hook.getBefore() : hook.getAfter();
        return singletonList(createTag(hook.getRef(), hash, hook.getCommits().get(0).getTimestamp().getTime()));
    }
}
