package argelbargel.jenkins.plugins.gitlab_branch_source.api;

@SuppressWarnings("unused")
public final class GitLabVersion {
    private String version;
    private String revision;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return version;
    }
}
