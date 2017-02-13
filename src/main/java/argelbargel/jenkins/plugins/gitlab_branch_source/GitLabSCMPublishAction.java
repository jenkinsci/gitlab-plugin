package argelbargel.jenkins.plugins.gitlab_branch_source;


import com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState;
import hudson.model.InvisibleAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode.RESULT;
import static argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode.STAGES;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.canceled;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.failed;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.running;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.success;
import static hudson.model.Result.ABORTED;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.util.logging.Level.SEVERE;


class GitLabSCMPublishAction extends InvisibleAction implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMPublishAction.class.getName());

    private final String publisherName;
    private final boolean markUnstableAsSuccess;
    private final boolean updateBuildDescription;
    private final BuildStatusPublishMode mode;

    GitLabSCMPublishAction(boolean updateBuildDescription, BuildStatusPublishMode mode, boolean markUnstableAsSuccess, String publisherName) {
        this.publisherName = publisherName;
        this.markUnstableAsSuccess = markUnstableAsSuccess;
        this.updateBuildDescription = updateBuildDescription;
        this.mode = mode;
    }

    void updateBuildDescription(Run<?, ?> build, GitLabSCMCauseAction action, TaskListener listener) {
        if (updateBuildDescription && !StringUtils.isBlank(action.getDescription())) {
            try {
                build.setDescription(action.getDescription());
            } catch (IOException e) {
                listener.getLogger().println("Failed to set build description");
            }
        }
    }

    void publishStarted(Run<?, ?> build, GitLabSCMCauseAction cause) {
        if (build instanceof WorkflowRun && mode == STAGES) {
            attachGraphListener((WorkflowRun) build, new GitLabSCMGraphListener(build, cause));
        } else if (mode == RESULT) {
            build.addAction(new RunningContextsAction(publisherName));
            publishBuildStatus(build, cause, publisherName, running, cause.getDescription());
        }
    }

    private void attachGraphListener(final WorkflowRun build, final GraphListener listener) {
        build.getExecutionPromise().addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        build.addAction(new RunningContextsAction());
                        FlowExecution execution = build.getExecution();
                        if (execution != null) {
                            execution.addListener(listener);
                        } else {
                            LOGGER.log(SEVERE, "could not get flow-execution for build " + build.getFullDisplayName());
                        }
                    }
                },
                Executors.newSingleThreadExecutor());
    }

    void publishResult(Run<?, ?> build, GitLabSCMCauseAction cause) {
        Result buildResult = build.getResult();
        if ((buildResult == SUCCESS) || ((buildResult == UNSTABLE) && markUnstableAsSuccess)) {
            updateRunningContexts(build, cause, success);
        } else if (buildResult == ABORTED) {
            updateRunningContexts(build, cause, canceled);
        } else {
            updateRunningContexts(build, cause, failed);
        }
    }

    private void updateRunningContexts(Run<?, ?> build, GitLabSCMCauseAction cause, BuildState state) {
        for (String context : build.getAction(RunningContextsAction.class).clear()) {
            publishBuildStatus(build, cause, context, state, "");
        }
    }

    private void publishBuildStatus(Run<?, ?> build, GitLabSCMCauseAction cause, String context, BuildState state, String description) {
        GitLabSCMBuildStatusPublisher.instance()
                .publish(build, context, cause.getProjectId(), cause.getRef(), cause.getHash(), state, description);
    }


    private final class GitLabSCMGraphListener implements GraphListener {
        private final Run<?, ?> build;
        private final GitLabSCMCauseAction cause;


        GitLabSCMGraphListener(Run<?, ?> build, GitLabSCMCauseAction cause) {
            this.build = build;
            this.cause = cause;
        }

        @Override
        public void onNewHead(FlowNode node) {
            if (isNamedStageStartNode(node)) {
                publishBuildStatus(build, cause, getRunningContexts().push(node), running, "");
            } else if (isStageEndNode(node, getRunningContexts().peekNodeId())) {
                publishBuildStatus(build, cause, getRunningContexts().pop(), success, "");
            }
        }

        private boolean isStageEndNode(FlowNode node, String startNodeId) {
            return startNodeId != null && node instanceof StepEndNode && ((StepEndNode) node).getStartNode().getId().equals(startNodeId);
        }

        private boolean isNamedStageStartNode(FlowNode node) {
            return node instanceof StepStartNode && Objects.equals(((StepStartNode) node).getStepName(), "Stage") && !Objects.equals(node.getDisplayFunctionName(), "stage");
        }

        private RunningContextsAction getRunningContexts() {
            return build.getAction(RunningContextsAction.class);
        }
    }


    private static final class RunningContextsAction extends InvisibleAction implements Serializable {
        private final Stack<String> nodeIds;
        private final LinkedHashMap<String, String> contexts;
        private int stageCount = 0;

        RunningContextsAction() {
            nodeIds = new Stack<>();
            contexts = new LinkedHashMap<>();
        }

        RunningContextsAction(String context) {
            this();
            contexts.put(context, context);
        }

        String push(FlowNode node) {
            return push(node.getId(), node.getDisplayName());
        }

        private String push(String id, String name) {
            nodeIds.push(id);
            String context = "#" + (++stageCount) + " " + name;
            contexts.put(id, context);
            return context;

        }

        String peekNodeId() {
            return !nodeIds.isEmpty() ? nodeIds.peek() : null;
        }

        String pop() {
            String nodeId = nodeIds.pop();
            return contexts.remove(nodeId);
        }

        Collection<String> clear() {
            List<String> names = new ArrayList<>(contexts.values());

            nodeIds.clear();
            contexts.clear();

            Collections.reverse(names);
            return names;
        }
    }
}
