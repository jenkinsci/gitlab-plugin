package com.dabsquared.gitlabjenkins.workflow.pipeline

// this pipeline is for multi-master multi-gitlab-project configuration
// there are 2 gitlab project on 2 different sites
// there are 2 jenkins instance.
//
// The idea is to have 1 pipeline script which contains a list of all build stages for each gitlab projects involved
// but it can change a behaviour de pending on which jenkins server it is being executed
//

def builds = [
    // jenkins master on site 1
    [name:"stage1-jenkins-site1", connection:gitLabConnection('test-connection' ), projectId:'test/test',  revisionHash:'master'],
    [name:"stage1-jenkins-site1", connection:gitLabConnection('test-connection2'), projectId:'test/utils', revisionHash:'master'],
    [name:"stage2-jenkins-site1", connection:gitLabConnection('test-connection' ), projectId:'test/test',  revisionHash:'master'],
    [name:"stage2-jenkins-site1", connection:gitLabConnection('test-connection2'), projectId:'test/utils', revisionHash:'master'],
    [name:"stage3-jenkins-site1", connection:gitLabConnection('test-connection' ), projectId:'test/test',  revisionHash:'master'],
    [name:"stage3-jenkins-site1", connection:gitLabConnection('test-connection2'), projectId:'test/utils', revisionHash:'master'],

    // jenkins master on site 2
    [name:"stage1-jenkins-site2", connection:gitLabConnection('test-connection' ), projectId:'test/test',  revisionHash:'master'],
    [name:"stage1-jenkins-site2", connection:gitLabConnection('test-connection2'), projectId:'test/utils', revisionHash:'master'],
    [name:"stage2-jenkins-site2", connection:gitLabConnection('test-connection' ), projectId:'test/test',  revisionHash:'master'],
    [name:"stage2-jenkins-site2", connection:gitLabConnection('test-connection2'), projectId:'test/utils', revisionHash:'master'],
    [name:"stage3-jenkins-site2", connection:gitLabConnection('test-connection' ), projectId:'test/test',  revisionHash:'master'],
    [name:"stage3-jenkins-site2", connection:gitLabConnection('test-connection2'), projectId:'test/utils', revisionHash:'master'],
]

// this is an example of how to filter builds list using jenkins url
def site_builds = builds.findAll{
  env.JENKINS_URL =~ /^http:\/\/localhost/ && it.name.contains('jenkins-site1')
}

echo """
  env.JENKINS_URL = ${env.JENKINS_URL}
  site_builds = $site_builds
"""

gitlabBuilds(builds: site_builds.collect{it.name}) {
  stage("stage1") {
    glBuilds =
    gitlabCommitStatus(builds: site_builds.findAll{it.name.contains('stage1')}) {
      echo "This is stage1"
    }
  }

  stage("stage2") {
    gitlabCommitStatus(builds: site_builds.findAll{it.name.contains('stage2')}) {
      echo "This is stage2"
    }
  }

  stage("stage3") {
    gitlabCommitStatus(builds: site_builds.findAll{it.name.contains('stage3')}) {
      echo "This is stage3"
    }
  }

}
