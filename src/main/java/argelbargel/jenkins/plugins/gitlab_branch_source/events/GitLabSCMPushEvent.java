package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMHead;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;


public class GitLabSCMPushEvent extends GitLabSCMHeadEvent {
    public GitLabSCMPushEvent(String id, PushHook hook) {
        this(Type.UPDATED, id, hook);
    }

    GitLabSCMPushEvent(Type type, String id, PushHook hook) {
        super(type, hook, id);
    }

    @Override
    protected Map<SCMHead, SCMRevision> heads(@Nonnull GitLabSCMSource source) throws IOException, InterruptedException {
        String ref = getPayload().getRef();
        GitLabSCMHead head = source.createBranch(ref, getPayload().getAfter());
        return Collections.<SCMHead, SCMRevision>singletonMap(head, head.getRevision());
    }

}
