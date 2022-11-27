#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(
  useContainerAgent: true,
  // Opt-in to the Artifact Caching Proxy, to be removed when it will be in opt-out.
  // See https://github.com/jenkins-infra/helpdesk/issues/2752 for more details and updates.
  artifactCachingProxyEnabled: true,
  configurations: [
    [platform: 'linux',   jdk: '17', jenkins: '2.375'],
    [platform: 'linux',   jdk: '11', jenkins: '2.361.4'],
    [platform: 'windows', jdk:  '8']
  ]
)
