package com.dabsquared.gitlabjenkins.workflow.pipeline

def builds = ['pre-build, build']

node {
  gitlabBuilds(builds: builds) {
    gitlabCommitStatus(name: 'pre-build'){
      echo 'this is pre-build stage'
    }
    gitlabCommitStatus(name: 'build'){
      echo 'this is build stage'
    }
  }  // gitlabBuidls(buids: builds)
}
