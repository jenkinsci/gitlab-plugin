package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.emptyMap;
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
    protected Map<SCMHead, SCMRevision> heads(@Nonnull GitLabSCMSource source) {
        if (!source.getMonitorTags()) {
            return emptyMap();
        }

        String hash = getType() == REMOVED ? getPayload().getBefore() : getPayload().getAfter();
        GitLabSCMHead head = source.createTag(getPayload().getRef(), hash);
        return Collections.<SCMHead, SCMRevision>singletonMap(head, head.getRevision());
    }
}
