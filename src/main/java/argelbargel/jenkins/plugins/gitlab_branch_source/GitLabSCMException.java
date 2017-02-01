package argelbargel.jenkins.plugins.gitlab_branch_source;

public class GitLabSCMException extends RuntimeException {
    public GitLabSCMException(String message) {
        this(message, null);
    }

    public GitLabSCMException(String message, Throwable cause) {
        super(message, cause);
    }
}
