package com.dabsquared.gitlabjenkins.workflow.pipeline

//properties([
//    gitLabConnection('test-connection')
//])

node {
  gitlabCommitStatus {
    echo 'this is simple jenkins-build'
  }
}

