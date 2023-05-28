#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(
  // Container agents start faster and are easier to administer
  useContainerAgent: true,
  // Show failures on all configurations
  failFast: false,
  // Test Java 11 with a recent LTS, Java 17 on Windows (not ready to test newer LTS versions)
  configurations: [
    [platform: 'linux',   jdk: '11'], // Linux first for coverage report on ci.jenkins.io
    [platform: 'windows', jdk: '17'],
  ],
  // TODO We anticipate that the migration will cause tests to start failing due to problems in
  // the test framework and not problems in production code. To avoid unnecessary noise during
  // the development process, we skip running tests until such a time that src/main is
  // reasonably stable with the new framework.
  tests: [
    skip: true // skip tests
  ]
)
