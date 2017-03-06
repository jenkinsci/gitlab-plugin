package argelbargel.jenkins.plugins.gitlab_branch_source.actions;


import argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode;
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

import static argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode.result;
import static argelbargel.jenkins.plugins.gitlab_branch_source.BuildStatusPublishMode.stages;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.canceled;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.failed;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.running;
import static com.dabsquared.gitlabjenkins.gitlab.api.model.BuildState.success;
import static hudson.model.Result.ABORTED;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.util.logging.Level.SEVERE;


public final class GitLabSCMPublishAction extends InvisibleAction implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(GitLabSCMPublishAction.class.getName());

    private final String publisherName;
    private final boolean markUnstableAsSuccess;
    private final boolean updateBuildDescription;
    private final BuildStatusPublishMode mode;

    public GitLabSCMPublishAction(boolean updateBuildDescription, BuildStatusPublishMode mode, boolean markUnstableAsSuccess, String publisherName) {
        this.publisherName = publisherName;
        this.markUnstableAsSuccess = markUnstableAsSuccess;
        this.updateBuildDescription = updateBuildDescription;
        this.mode = mode;
    }

    public void updateBuildDescription(Run<?, ?> build, String description, TaskListener listener) {
        if (updateBuildDescription && !StringUtils.isBlank(description)) {
            try {
                build.setDescription(description);
            } catch (IOException e) {
                listener.getLogger().println("Failed to set build description");
            }
        }
    }

    public void publishStarted(Run<?, ?> build, GitLabSCMHeadMetadataAction metadata, String description) {
        if (build instanceof WorkflowRun && mode == stages) {
            attachGraphListener((WorkflowRun) build, new GitLabSCMGraphListener(build, metadata));
        } else if (mode == result) {
            build.addAction(new RunningContextsAction(publisherName));
            publishBuildStatus(build, metadata, publisherName, running, description);
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

    public void publishResult(Run<?, ?> build, GitLabSCMHeadMetadataAction metadata) {
        Result buildResult = build.getResult();
        if ((buildResult == SUCCESS) || ((buildResult == UNSTABLE) && markUnstableAsSuccess)) {
            updateRunningContexts(build, metadata, success);
        } else if (buildResult == ABORTED) {
            updateRunningContexts(build, metadata, canceled);
        } else {
            updateRunningContexts(build, metadata, failed);
        }
    }

    private void updateRunningContexts(Run<?, ?> build, GitLabSCMHeadMetadataAction metadata, BuildState state) {
        for (String context : build.getAction(RunningContextsAction.class).clear()) {
            publishBuildStatus(build, metadata, context, state, "");
        }
    }

    private void publishBuildStatus(Run<?, ?> build, GitLabSCMHeadMetadataAction metadata, String context, BuildState state, String description) {
        GitLabSCMBuildStatusPublisher.instance()
                .publish(build, context, metadata.getProjectId(), metadata.getRef(), metadata.getHash(), state, description);
    }


    private final class GitLabSCMGraphListener implements GraphListener {
        private final Run<?, ?> build;
        private final GitLabSCMHeadMetadataAction metadata;


        GitLabSCMGraphListener(Run<?, ?> build, GitLabSCMHeadMetadataAction metadata) {
            this.build = build;
            this.metadata = metadata;
        }

        @Override
        public void onNewHead(FlowNode node) {
            if (isNamedStageStartNode(node)) {
                publishBuildStatus(build, metadata, getRunningContexts().push(node), running, "");
            } else if (isStageEndNode(node, getRunningContexts().peekNodeId())) {
                publishBuildStatus(build, metadata, getRunningContexts().pop(), success, "");
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
