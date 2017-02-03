package argelbargel.jenkins.plugins.gitlab_branch_source.hooks;

import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMNavigator;
import argelbargel.jenkins.plugins.gitlab_branch_source.GitLabSCMSource;
import argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabHookEventType;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMNavigatorOwner;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static argelbargel.jenkins.plugins.gitlab_branch_source.api.GitLabHookEventType.byHeader;


@Extension
public final class GitLabSCMWebHook implements UnprotectedRootAction {
    static final String HOOK_PATH_SEP = "/";
    @SuppressWarnings("WeakerAccess")
    static final String URL_NAME = "gitlab-scm";
    static final String NOTIFICATION_ENDPOINT = URL_NAME + HOOK_PATH_SEP + "notify";

    private static final Pattern HOOK_ID_PATTERN = Pattern.compile("^([^/]+)(" + Pattern.quote(HOOK_PATH_SEP) + "\\d+)?$");
    private static final String CHARSET_UTF_8 = "utf-8";
    private static final String GITLAB_EVENT_TYPE_HEADER = "X-Gitlab-Event";


    public static GitLabSCMWebHookListener createListener(GitLabSCMNavigator navigator) {
        return new GitLabSCMWebHookListener(navigator.getConnectionName(), 0);
    }

    public static GitLabSCMWebHookListener createListener(GitLabSCMSource source) {
        return new GitLabSCMWebHookListener(source.getConnectionName(), source.getProjectId());
    }

    public static GitLabSCMWebHook get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(GitLabSCMWebHook.class);
    }


    private final HookManager manager;
    private final HookHandler handler;

    public GitLabSCMWebHook() {
         manager = new HookManager();
         handler = new HookHandler();
         new Thread(new ListenerInitializerTask()).start();
    }

    public void addListener(GitLabSCMNavigator navigator) {
        manager.addListener(navigator.getHookListener(), navigator.getRegisterWebHooks());
    }

    public void addListener(GitLabSCMSource source) {
        manager.addListener(source.getHookListener(), source.getRegisterWebHooks());
    }

    public void removeListener(GitLabSCMNavigator navigator) {
        manager.removeListener(navigator.getHookListener(), navigator.getRegisterWebHooks());
    }

    public void removeListener(GitLabSCMSource source) {
        manager.removeListener(source.getHookListener(), source.getRegisterWebHooks());
    }


    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return URL_NAME;
    }


    @SuppressWarnings("unused")
    @RequirePOST
    public HttpResponse doNotify(StaplerRequest req) {
        try {
            handler.handle(extractListenerId(req), getEventType(req), getRequestBody(req));
            return HttpResponses.ok();
        } catch (IllegalArgumentException e) {
            throw HttpResponses.error(400, "bad request: " + e.getMessage());
        } catch (Exception e) {
            throw HttpResponses.error(500, e.getMessage());
        }
    }

    private String extractListenerId(StaplerRequest req) {
        String path = req.getRestOfPath();
        String id = getListenerId(path.substring(1));
        if (id == null) {
            throw HttpResponses.notFound();
        }

        return id;
    }

    private String getListenerId(String id) {
        if (manager.hasListener(id)) {
            return id;
        }

        // unknown project-hooks (<connection-id>/<project-id>) are redirected to navigator (<connection-id>)
        Matcher m = HOOK_ID_PATTERN.matcher(id);
        if (m.matches() && manager.hasListener(m.group(1))) {
            return m.group(1);
        }

        return null;

    }

    private GitLabHookEventType getEventType(StaplerRequest req) {
        try {
            return byHeader(req.getHeader(GITLAB_EVENT_TYPE_HEADER));
        } catch (NoSuchElementException e) {
            throw HttpResponses.error(400, "bad request, " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private String getRequestBody(StaplerRequest request) {
        try {
            String charset = request.getCharacterEncoding() == null ? CHARSET_UTF_8 : request.getCharacterEncoding();
            String requestBody = IOUtils.toString(request.getInputStream(), charset);
            if (StringUtils.isBlank(requestBody)) {
                throw HttpResponses.error(400, "bad-request, request-body is empty");
            }

            return requestBody;
        } catch (IOException e) {
            throw HttpResponses.error(500, "Failed to read request body");
        }
    }

    private class ListenerInitializerTask implements Runnable {
        @Override
        public void run() {
            for (SCMNavigatorOwner owner : Jenkins.getInstance().getAllItems(SCMNavigatorOwner.class)) {
                for (SCMNavigator navigator : owner.getSCMNavigators()) {
                    if (navigator instanceof GitLabSCMNavigator) {
                        if (!StringUtils.isEmpty(((GitLabSCMNavigator) navigator).getConnectionName())) {
                            addListener((GitLabSCMNavigator) navigator);
                        }
                    }
                }
            }

            for (SCMSourceOwner owner : Jenkins.getInstance().getAllItems(SCMSourceOwner.class)) {
                for (SCMSource source : owner.getSCMSources()) {
                    if (source instanceof GitLabSCMSource) {
                        addListener((GitLabSCMSource) source);
                    }
                }
            }
        }
    }
}
