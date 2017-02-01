package argelbargel.jenkins.plugins.gitlab_branch_source.events;

import com.dabsquared.gitlabjenkins.cause.CauseData;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.Commit;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.MergeRequestObjectAttributes;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.PushHook;
import org.eclipse.jgit.util.StringUtils;

import java.util.List;

import static com.dabsquared.gitlabjenkins.cause.CauseDataBuilder.causeData;

class CauseDataHelper {
    static CauseData buildCauseData(PushHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.PUSH)
                .withSourceProjectId(hook.getProjectId())
                .withTargetProjectId(hook.getProjectId())
                .withBranch(getTargetBranch(hook))
                .withSourceBranch(getTargetBranch(hook))
                .withUserName(hook.getUserName())
                .withUserEmail(hook.getUserEmail())
                .withSourceRepoHomepage(hook.getRepository().getHomepage())
                .withSourceRepoName(hook.getRepository().getName())
                .withSourceNamespace(hook.getProject().getNamespace())
                .withSourceRepoUrl(hook.getRepository().getUrl())
                .withSourceRepoSshUrl(hook.getRepository().getGitSshUrl())
                .withSourceRepoHttpUrl(hook.getRepository().getGitHttpUrl())
                .withMergeRequestTitle("")
                .withMergeRequestDescription("")
                .withMergeRequestId(null)
                .withMergeRequestIid(null)
                .withTargetBranch(getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withTriggeredByUser(retrievePushedBy(hook))
                .withBefore(hook.getBefore())
                .withAfter(hook.getAfter())
                .withLastCommit(hook.getAfter())
                .withTargetProjectUrl(hook.getProject().getWebUrl())
                .build();
    }

    static CauseData buildCauseData(MergeRequestHook hook) {
        return buildCauseData(hook.getObjectAttributes());
    }

    private static CauseData buildCauseData(MergeRequestObjectAttributes attributes) {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(attributes.getSourceProjectId())
                .withTargetProjectId(attributes.getTargetProjectId())
                .withBranch(attributes.getTargetBranch())
                .withSourceBranch(attributes.getSourceBranch())
                .withUserName(attributes.getLastCommit().getAuthor().getName())
                .withUserEmail(attributes.getLastCommit().getAuthor().getEmail())
                .withSourceRepoHomepage(attributes.getSource().getWebUrl())
                .withSourceRepoName(attributes.getSource().getName())
                .withSourceNamespace(attributes.getSource().getNamespace())
                .withSourceRepoUrl(attributes.getSource().getUrl())
                .withSourceRepoSshUrl(attributes.getSource().getSshUrl())
                .withSourceRepoHttpUrl(attributes.getSource().getHttpUrl())
                .withMergeRequestTitle(attributes.getTitle())
                .withMergeRequestDescription(attributes.getDescription())
                .withMergeRequestId(attributes.getId())
                .withMergeRequestIid(attributes.getIid())
                .withTargetBranch(attributes.getTargetBranch())
                .withTargetRepoName(attributes.getTarget().getName())
                .withTargetNamespace(attributes.getTarget().getNamespace())
                .withTargetRepoSshUrl(attributes.getTarget().getSshUrl())
                .withTargetRepoHttpUrl(attributes.getTarget().getHttpUrl())
                .withTriggeredByUser(attributes.getLastCommit().getAuthor().getName())
                .withLastCommit(attributes.getLastCommit().getId())
                .withTargetProjectUrl(attributes.getTarget().getUrl())
                .build();
    }

    private static String getTargetBranch(PushHook hook) {
        return hook.getRef() == null ? null : hook.getRef().replaceFirst("^refs/heads/", "");
    }

    private static String retrievePushedBy(final PushHook hook) {
        String userName = hook.getUserName();
        if (!StringUtils.isEmptyOrNull(userName)) {
            return userName;
        }

        final List<Commit> commits = hook.getCommits();
        if (commits != null && !commits.isEmpty()) {
            return commits.get(commits.size() - 1).getAuthor().getName();
        }

        return null;
    }
}
