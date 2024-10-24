package com.dabsquared.gitlabjenkins.workflow.jenkinsFile

pipeline {
  agent any
  triggers {
    gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
  }
  stages {
    stage("build") {
      steps {
        if (GitLabMergeRequestLabelExists("test label"))
        {
          echo "test label found"
        }
        else
        {
          echo "test label not found"
        }
      }
    }
  }
}
