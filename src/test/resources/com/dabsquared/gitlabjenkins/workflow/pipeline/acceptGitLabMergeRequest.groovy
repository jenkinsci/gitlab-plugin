package com.dabsquared.gitlabjenkins.workflow.pipeline

properties([
    gitLabConnection('test-connection')
])

node {
    acceptGitLabMR (
        mergeCommitMessage : 'Merge commit message',
        useMRDescription : true,
        removeSourceBranch : true
    )
    echo 'this is simple jenkins-build'
}


