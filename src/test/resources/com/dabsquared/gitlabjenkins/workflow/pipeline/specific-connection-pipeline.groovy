package com.dabsquared.gitlabjenkins.workflow.pipeline

def builds = ['pre-build', 'build']

node('master') {
  gitlabBuilds(builds: builds) {
    gitlabCommitStatus(name: 'pre-build', connection: gitLabConnection('test-connection')) {
      echo 'this is pre-build stage'
    }
    gitlabCommitStatus(name: 'build',connection: gitLabConnection('test-connection')) {
      echo 'this is build stage'
    }
  }
}
