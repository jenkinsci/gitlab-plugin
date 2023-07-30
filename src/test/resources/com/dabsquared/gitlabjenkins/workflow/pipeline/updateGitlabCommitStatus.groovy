package com.dabsquared.gitlabjenkins.workflow.pipeline

properties([
    gitLabConnection('test-connection')
])

node {
  updateGitlabCommitStatus name: 'build', state: 'PENDING'
  echo 'this is simple jenkins-build'
  updateGitlabCommitStatus name: 'build', state: 'SUCCESS'
}
