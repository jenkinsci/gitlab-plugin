package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory;
import com.google.common.base.Predicate;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.CommitBuilder.commit;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestHookBuilder.mergeRequestHook;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.ProjectBuilder.project;
import static com.dabsquared.gitlabjenkins.gitlab.hook.model.builder.generated.UserBuilder.user;
import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.dabsquared.gitlabjenkins.trigger.filter.MergeRequestLabelFilterFactory.newMergeRequestLabelFilter;

/**
 * Intended to be the primary means to start off a merge request hook for testing purposes.
 * Required if plugin logic needs to be tested - Jenkins-initiated scheduled builds will not
 * have the necessary data for the plugin to function.
 *
 * @author benjie332
 */
public class HookTrigger {

    public static void triggerHookSynchronously(ProjectSetupResult project,
                                                Git git,
                                                RevCommit lastCommit) throws InterruptedException, GitAPIException, IOException {

        triggerHook(project, git, lastCommit, true);

    }

    public static void triggerHookWithoutWaitingForResult(ProjectSetupResult project,
                                                          Git git,
                                                          RevCommit lastCommit) throws InterruptedException, GitAPIException, IOException {
        triggerHook(project, git, lastCommit, false);
    }

    private static void triggerHook(ProjectSetupResult project,
                                   Git git,
                                   RevCommit lastCommit,
                                   boolean blocking) throws IOException, GitAPIException, InterruptedException {
        ObjectId head = git.getRepository().resolve(Constants.HEAD);

        MergeRequestHookTriggerHandlerFactory.newMergeRequestHookTriggerHandler(true, TriggerOpenMergeRequest.source, false).handle(project.getTestProject(), mergeRequestHook()
            .withProject(project().withWebUrl("https://gitlab.org/test.git").build())
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withTargetBranch("refs/heads/" + git.nameRev().add(head).call().get(head))
                .withState(State.opened)
                .withIid(1)
                .withTitle("test")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
                .withUrl("https://gitlab.org/test.git")
                .withLastCommit(commit().withAuthor(user().withName("test").build()).withId(lastCommit.getName()).build())
                .withSource(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .withWebUrl("https://gitlab.org/test.git")
                    .build())
                .withTarget(project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitlab.org/test")
                    .withUrl("git@gitlab.org:test.git")
                    .withSshUrl("git@gitlab.org:test.git")
                    .withHttpUrl("https://gitlab.org/test.git")
                    .withWebUrl("https://gitlab.org/test.git")
                    .build())
                .build())
            .build(),
            false,
            BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),newMergeRequestLabelFilter(null));

        if (blocking) {
            //wait for hook to register build event - intended to make sure the trigger actually fires a build
            project.getBuildNotifier().getLock().block(5000);
            //....here we actually wait for the result of the build
            blockUntilRunComplete(project.getTestProject());
        }
    }

    private static void blockUntilRunComplete(FreeStyleProject testProject) throws InterruptedException {
        int retries = 0;
        while(testProject.getBuilds().filter(new Predicate<FreeStyleBuild>() {
            @Override
            public boolean apply(@Nullable FreeStyleBuild freeStyleBuild) {
                return (freeStyleBuild != null && !freeStyleBuild.isBuilding());
            }
        }).isEmpty() || retries < 5) {
            Thread.sleep(500L);
            retries++;
        }
    }
}
