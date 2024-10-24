package com.dabsquared.gitlabjenkins.workflow.jenkinsFile

node {
  echo "build started"
  if (GitLabMergeRequestLabelExists("test label"))
  {
    echo "test label found"
  }
  else
  {
    echo "test label not found"
  }
}
