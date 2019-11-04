package com.dabsquared.gitlabjenkins.workflow.jenkinsFile

pipeline {
  agent any
  post {
    failure {
      updateGitlabCommitStatus name: 'build', state: 'failed'
    }
    success {
      updateGitlabCommitStatus name: 'build', state: 'success'
    }
  }
  options {
    gitLabConnection('test-connection',null,null)
  }
  triggers {
    gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
  }
  stages {
    stage("build") {
      steps {
        echo "hello world"
      }
    }
  }
}
