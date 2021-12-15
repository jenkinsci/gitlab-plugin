package com.dabsquared.gitlabjenkins.workflow.pipeline

def builds = ['pre-build', 'build']

node('master') {
  gitlabBuilds(builds: builds) {
    gitlabCommitStatus(name: 'pre-build',
        connection: gitLabConnection(gitLabConnection:'test-connection'),
        builds: [
            [projectId: 'test/test', revisionHash: 'master'],
            [projectId: 'test/utils', revisionHash: 'master'],
    ]) {
      echo 'this is pre-build stage'
    }

    gitlabCommitStatus(name: 'build',
        connection: gitLabConnection(gitLabConnection:'test-connection'),
        builds: [
            [projectId: 'test/test', revisionHash: 'master'],
            [projectId: 'test/utils', revisionHash: 'master'],
    ]) {
      echo 'this is build stage'
    }
  }
}
