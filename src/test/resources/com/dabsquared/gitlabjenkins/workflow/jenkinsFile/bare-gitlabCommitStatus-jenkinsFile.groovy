package com.dabsquared.gitlabjenkins.workflow.jenkinsFile

properties([
    gitLabConnection('test-connection')
])

node {
  git 'http://gitlab/test/test.git'
  gitlabCommitStatus {
    echo 'this is pre-build stage'
  }
}

pipeline {
  agent any
  options {
    gitLabConnection('test-connection')
  }
  triggers {
    gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
  }
  stages {
    stage("build") {
      steps {
        gitlabCommitStatus {
          echo "hello world"
        }
      }
    }
  }
}
