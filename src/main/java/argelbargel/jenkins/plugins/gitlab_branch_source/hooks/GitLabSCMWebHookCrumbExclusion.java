package argelbargel.jenkins.plugins.gitlab_branch_source.hooks;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("unused")
@Extension
public final class GitLabSCMWebHookCrumbExclusion extends CrumbExclusion {
    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith('/' + GitLabSCMWebHook.NOTIFICATION_ENDPOINT)) {
            chain.doFilter(req, resp);
            return true;
        }
        return false;
    }
}
