#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(
  // Run a JVM per core in tests
  forkCount: '1C',
  // Container agents start faster and are easier to administer
  useContainerAgent: true,
  // Show failures on all configurations
  failFast: false,
  // Test Java 17 and 21
  configurations: [
    [platform: 'linux',   jdk: '21'], // Linux first for coverage report on ci.jenkins.io
    [platform: 'windows', jdk: '17'],
  ]
)
