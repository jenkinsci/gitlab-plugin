/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(
  // Container agents start faster and are easier to administer
  useContainerAgent: true,
  // Show failures on all configurations
  failFast: false,
  // Opt-in to the Artifact Caching Proxy, to be removed when it will be in opt-out.
  // See https://github.com/jenkins-infra/helpdesk/issues/2752 for more details and updates.
  artifactCachingProxyEnabled: true,
  // Test Java 11 with a recent LTS, Java 17 even more recent
  configurations: [
    [platform: 'linux', jdk: 17],
    [platform: 'windows', jdk: 11],
  ]
)
