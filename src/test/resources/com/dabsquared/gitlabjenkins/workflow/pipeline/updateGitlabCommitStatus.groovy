package com.dabsquared.gitlabjenkins.workflow.pipeline

properties([
    gitLabConnection('test-connection')
])

node('master') {
  updateGitlabCommitStatus name: 'build', state: 'pending'
  echo 'this is simple jenkins-build'
  updateGitlabCommitStatus name: 'build', state: 'success'
}
