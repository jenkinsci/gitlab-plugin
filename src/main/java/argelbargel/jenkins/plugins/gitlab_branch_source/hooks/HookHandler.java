package argelbargel.jenkins.plugins.gitlab_branch_source.hooks;

import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabHookEventType;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.SystemHook;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMMergeRequestEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMPushEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMSourceEvent;
import argelbargel.jenkins.plugins.gitlab_branch_source.events.GitLabSCMTagPushEvent;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.WebHook;
import com.dabsquared.gitlabjenkins.util.JsonUtil;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMSourceEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabHookEventType.PUSH;

class HookHandler {
    private static final Logger LOGGER = Logger.getLogger(HookHandler.class.getName());

    void handle(String id, GitLabHookEventType eventType, String body) {
        switch (eventType) {
            case PUSH:
                SCMHeadEvent.fireNow(new GitLabSCMPushEvent(id, readHook(PushHook.class, body)));
                break;
            case TAG_PUSH:
                SCMHeadEvent.fireNow(new GitLabSCMTagPushEvent(id, readHook(PushHook.class, body)));
                break;
            case MERGE_REQUEST:
                SCMHeadEvent.fireNow(GitLabSCMMergeRequestEvent.create(id, readHook(MergeRequestHook.class, body)));
                break;
            case SYSTEM_HOOK:
                handleSystemHook(id, body);
                break;
            default:
                throw new IllegalArgumentException("cannot handle hook-event of type " + eventType);
        }
    }

    private void handleSystemHook(String id, String body) {
        try {
            LOGGER.fine("handling system-hook for " + id + ": " + body);
            SystemHook hook = readHook(SystemHook.class, body);
            if (hook.getEventName().equals("push")) {
                handle(id, PUSH, body);
            } else {
                SCMSourceEvent.fireNow(GitLabSCMSourceEvent.create(id, hook));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warning("ignoring system hook: " + e.getMessage());
        }
    }

    private <T extends WebHook> T readHook(Class<T> type, String body) {
        try {
            return JsonUtil.read(body, type);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "could not create hook from request-body", e);
            throw new IllegalArgumentException("ould not read payload");
        }
    }
}
