/*
 * See the documentation for more options:
 * https://github.com/jenkins-infra/pipeline-library/
 */
buildPlugin(useContainerAgent: true, configurations: [
  // Test the long-term support end of the compatibility spectrum (i.e., the minimum required
  // Jenkins version).
  [ platform: 'linux', jdk: '8', jenkins: null ],

  // Test the common case (i.e., a recent LTS release) on both Linux and Windows.
  [ platform: 'linux', jdk: '8', jenkins: '2.303.2' ],
  [ platform: 'windows', jdk: '8', jenkins: '2.303.2' ],

  // Test the bleeding edge of the compatibility spectrum (i.e., the latest supported Java runtime).
  [ platform: 'linux', jdk: '11', jenkins: '2.303.2' ],
])
