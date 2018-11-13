package com.dabsquared.gitlabjenkins.workflow.pipeline

//properties([
//    gitLabConnection('test-connection')
//])

node('master') {
  gitlabCommitStatus {
    echo 'this is simple jenkins-build'
  }
}

