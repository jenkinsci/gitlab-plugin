package com.dabsquared.gitlabjenkins.workflow.pipeline

def builds = ['pre-build', 'build']

node {
  gitlabBuilds(builds: builds) {
    gitlabCommitStatus(name: 'pre-build', connection: gitLabConnection('test-connection',null,null)) {
      echo 'this is pre-build stage'
    }
    gitlabCommitStatus(name: 'build', connection: gitLabConnection('test-connection',null,null)) {
      echo 'this is build stage'
    }
  }
}
