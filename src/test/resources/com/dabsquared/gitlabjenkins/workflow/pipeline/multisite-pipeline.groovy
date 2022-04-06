package com.dabsquared.gitlabjenkins.workflow.pipeline

// this pipeline is for multi-master multi-gitlab-project configuration
// there are 2 gitlab project on 2 different sites
// there are 2 jenkins instance.
//
// The idea is to have 1 pipeline script which contains a list of all build stages for each gitlab projects involved
// but it can change the behaviour depending on which jenkins server it is being executed
//
def site_1_connection = gitLabConnection(gitLabConnection:'test-connection')
def site_2_connection = gitLabConnection(gitLabConnection:'test-connection2')
def builds = [
    // jenkins master on site 1
    [name:"stage1-jenkins-site1", connection:site_1_connection, projectId:'test/test',  revisionHash:'master'],
    [name:"stage1-jenkins-site1", connection:site_2_connection, projectId:'test/utils', revisionHash:'master'],
    [name:"stage2-jenkins-site1", connection:site_1_connection, projectId:'test/test',  revisionHash:'master'],
    [name:"stage2-jenkins-site1", connection:site_2_connection, projectId:'test/utils', revisionHash:'master'],
    [name:"stage3-jenkins-site1", connection:site_1_connection, projectId:'test/test',  revisionHash:'master'],
    [name:"stage3-jenkins-site1", connection:site_2_connection, projectId:'test/utils', revisionHash:'master'],

    // jenkins master on site 2
    [name:"stage1-jenkins-site2", connection:site_1_connection, projectId:'test/test',  revisionHash:'master'],
    [name:"stage1-jenkins-site2", connection:site_2_connection, projectId:'test/utils', revisionHash:'master'],
    [name:"stage2-jenkins-site2", connection:site_1_connection, projectId:'test/test',  revisionHash:'master'],
    [name:"stage2-jenkins-site2", connection:site_2_connection, projectId:'test/utils', revisionHash:'master'],
    [name:"stage3-jenkins-site2", connection:site_1_connection, projectId:'test/test',  revisionHash:'master'],
    [name:"stage3-jenkins-site2", connection:site_2_connection, projectId:'test/utils', revisionHash:'master'],
]

// this is an example of how to filter builds list using jenkins url
def site_builds = []
def site_build_names = []
for(item in builds) {
  if(item.name.contains('jenkins-site1')) {
    site_builds.add(item)
    site_build_names.add(item.name)
  }
}

def stage_builds = []

node {
  gitlabBuilds(builds: site_build_names) {

    // in the test enviroment I cannot make it works with List.collect(Closure c) nor with List.findAll(Closure c)
    // however in the production instance of Jenkins I can use both of them e.g:
    // site_builds.findAll{it.name.contains('stage1')}
    stage_builds = []
    for(item in site_builds) {
      if(item.name.contains('stage1')) {
        stage_builds.add(item)
      }
    }

    gitlabCommitStatus(builds: stage_builds) {
      echo "this is stage1"
    }

    stage_builds = []
    for(item in site_builds) {
      if(item.name.contains('stage2')) {
        stage_builds.add(item)
      }
    }

    gitlabCommitStatus(builds: stage_builds) {
      echo "this is stage2"
    }

    stage_builds = []
    for(item in site_builds) {
      if(item.name.contains('stage3')) {
        stage_builds.add(item)
      }
    }

    gitlabCommitStatus(builds: stage_builds) {
      echo "this is stage3"
    }
  } // gitlabBuilds
}
