ChangeLog
=====================

1.4.1
=====================
* #410: `NoStaplerConstructorException` for `addGitLabMRComment`
* Use plugin ClassLoader for the resteasy client instead of the uberClassLoader
* #406: Add include/exclude of merge requests based on gitlab merge request labels
* Add possibility to configure secret tokens per job to allow only web hooks with the correct token to trigger builds
* #415: Add actual trigger phrase as environment variable

1.4.0
=====================
* Breaking changes for some Pipeline jobs (see [migration guide](https://github.com/jenkinsci/gitlab-plugin/wiki/Migration-Guides) for more information)
* Cleanup UI for GitLabPushTrigger
* #201: customize notes for merge requsts
* #168: MR Voting Broken after Gitlab 8.2
* #190: Add option to mark unstable builds as success in GitLab
* #345: Configurable "Add note with build status on merge requests"
* Add Notifier and workflow step for accepting a MR on success
* Add workflow step for adding comments to a MR

1.3.2
=====================
* JENKINS-36863: Credentials drop-down doesn't show API token credential! (finally fixed it)
* #402: Web hooks not triggering build in v1.3.1, Jenkins 2.20
* #404: NullPointerException with gitlab ce 8.11

1.3.1
=====================
* JENKINS-36863: Credentials drop-down doesn't show API token credential!
* #299: Upgrading the plugin sometimes causes loss of configuration
* #382: Outgoing HTTP Proxy support in gitlab-plugin
* #383: Pipeline - "Pending" Jobs for all stages
* #69: Link to gitlab merge request url in getShortDescription posted to jenkins
* #396: Jenkins job stuck publishing build status
* #400: Fix gitlabBuilds step to actually use 'pending'
* JENKINS-35258: ci-skip and author should use the latest commit, not the first one from the push event

1.3.0
=====================
* Drop official support for GitLab 7.14 -> no guarantee that new features will work for this version
* #298: Pipeline Multibranch builds are not supported
* #374/JENKINS-36347: Status publisher not updating status when "Merge before build" (git) fails

1.2.5
=====================
* #361: Avoid rebuild merge request if assignee changes
* #366: Jenkins can't install plugin

1.2.4
=====================
* #332: Find better fitting labels for the supported kind of GitLab credentials in global configuration
* #261: Support [WIP] tag to prevent builds from triggering
* #317: Retried builds are not marked immediately with Gitlab 8.1
* #306: GitLab [ci-skip] is ignored
* #362: Allow to change build name submitted to GitLab
* Catch client exceptions while retrieving the projectId from GitLab
* #358: Null pointer exception when using 'rebuild open MRs' and GitLab 7.14
* #364: gitlabCommitStatus: Gitlab in non-root location
* #213: Trigger build by phrase in merge request note
* #282: Authentication not required for /project end-point
* #359: Git repositories in dockerized GitLab cannot be reached from Jenkins when using plugin-provided url parameters
* JENKINS-35538: Update credentials-plugin to version 2.1.0
* #357: gitlabCommitStatus usage not clear
* #349: Fix NPE when updating commit status
* #342: Timeout when "Add note with build status on merge requests" runs
* Catch also ProcessingExceptions while using the GitLab client
* Change order of steps add note and accept MR
* #335: More tests and refactoring of ActionResolver.onPost

1.2.3
=====================
* #294 Fix: Do not trigger a build for remove branch push event
* #246, #290 Add gitlabMergeRequestIid, gitlabSourceNamespace and gitlabTargetNamespace to CauseData and show the correct MR id within the build description
* #281 Fix: Builds not be triggered on tag push events
* #304 Utilizing SpringUtils to safely perform string comparison
* #308 Use the character encoding of the request or UTF-8 if no character encoding is available for decoding the request body of a web hook
* #311 Fix finding related commit of the build
* #312 Fix setting MR IID
* #319: Wrong username in Jenkins build description on push event
* #322 Git push hook namespace compatibility for Gitlab pre ~v8.5
* #327 Fix to support NameSpace or ProjectName with dot in it
* Update tests for new commit status publisher behavior
* Use HTTP Get instead of HEAD to check for the existence of a commit as workaround for RESTEASY-1347
* Fix: HTTP 404 error for the rebuild open MRs on push to target branch for forked repositories
* Update list of variables available in builds
* Add notes to README about known Pipeline bugs
* Use ResteasyClientBuilder to configure ignoreCertificateErrors
* Configure connection pooling and timeouts for the client
* Docker: Update Jenkins version to 2.3
* Readme: Fix contents links
* Readme: Update branch name filtering section
* Readme: Add note on gitlab hook values injection
* Readme: Add known bugs section
* Readme: Note Jenkins parameter security update that can cause problems
* Readme: Explain how to add the GitLab API key as a credential.

1.2.2
=====================
* #283 Send thumbs-down icon when build fails
* #284 NullPointerException when using the GitLabCommitStatusPublisher
* Use jenkins credentials-plugin for storing the GitLab API Token
* Clear clients cache if connection config has changed
* Add workflow step that updates the GitLab commit status depending on the build status

1.2.1
=====================
* #271 Version 1.2.0 breaks support for Workflow/Pipeline jobs
 * **This version of the plugin is incompatible with earlier versions if you are using Pipeline jobs!** You will need to manually change your job configurations. Please see the README section on using the plugin with Pipeline for more information about this.
* #275 Handle GitLab API deprecations >= 8.5.0

1.2.0
=====================
* #217 Inconsistent code formatting
* #212 branch names with non-URL safe characters
* #211 Provide useful values of the Push and MR events as build variables
* #210 Listen for update MR events instead of push events to rebuild open MRs
* #203 Use JAX-RS and resteasy for implementing the GitLab API
* #179 NPE if description of MR event is null
* #175 Retrieve author name from MR event 'last_commit'
* #162 Build status for first push to new branch does not get reported to GitLab
* #159 Utilize "not_found" state for GitLab CI Merge Status page
* #136 gitlabSourceRepoURL from hook request
* #121 Configure GitLab host url and API token at job level
* #77 "Ignore SSL Certificate Errors" has no effect
* #231 Cleanup GitLabWebHook
* #230 Add functionality to configure multiple gitlab connections
* #229 Add publisher to update the commit/merge request status in GitLab
* #228 Cleanup GitLabPushTrigger

1.1.32
=====================
* #218 license within pom.xml and LICENSE is different
* #226 Add description to parameters actions
* #216 Fix NPE when author email is not available
* #235 mention required permissions for commit status API
* #225 Enhance README section Contribution
* #236 Fix the usage of CommitStatus
* #215 Fix error "failed to communicate with gitlab server ..." when try to rebuild open MRs

1.1.30 + 1.1.31
=====================
* issues with the release plugin

1.1.29
=====================
* #127 Added support for the GitLab 8.1 commit API
* #128 Add two variables gitlabUserName and gitlabUserEmail
* #129 Assorted minor fixes for merge requests
* #139 Fix for issue #125: use reponame for branch caching
* #140 Added missing documentation 8.1 CI features
* #141 Refactoring data objects to own package
* #146 Fixed Documentation after #128
* #149 New feature: Add support for regex based filtering and more
* #151 Update readme to reflect support for the commit status API
* #154 8.1 is now a supported version
* #163 Bugfix for issue #160: allow merge requests to different branches from one commit
* #164 Not reporting build status 8.1+
* #180 Parameter gitlabMergeRequestTitle is always blank
* #182 Status to canceled instead of failed if Jenkins build is aborted
* #195 Fix NPE if there is no assignee of the MR
* #197 Reorganize README to be more clear about config for different versions of GitLab
* #205 closes #183 Plugin not working with multiple SCM configuration
* #206 Fix Jenkins Workflow support in build trigger code
* #209 Don't ignore push builds when responding to status query

1.1.28
=====================
* (#119 - @mfriedenhagen) Add buildUrl in the description of the merge (shown in GitLab)
* (#124 - @jsyrjala) Fix acceptMergeRequestOnSuccess configuration so it stays set, and is disabled by default
* (#127 and #134 - @thommy101) Added support for the GitLab 8.1 commit API
* (#133 - @EmteZogaf) Send commit status on push-triggered merge request build

1.1.27
=====================
* (#118 - @christ66) IntelliJ Excludes
* (#117 - @christ66) Ball Color Changed to Result
* (#110 - @kasper-f) Accept Merge Request On Build Succes
* (#106 - @xathien) Null Pointer Error Fix
* (#105 - @jr4) Merge Request Not Built On First Raise

1.1.26
=====================
* (#101 - @TomAndrews) Generalise ci-skip
* (#102 - @TomAndrews) Configurable Rebuild Options

1.1.4
=====================
* Updated git-plugin dependency to latest version; minimum supported version of Jenkins is now 1.568
* Rebuild open merge requests after a push to the source branch (configurable parameter) (kasper-f)
* Build page link on Merge Request page redirects to the correct build (zenovich)
* Stop building closed merge requests
* Bug fixes and documentation updates

1.1.2
=====================
* Filter source branches (for push requests only)
* Show build status images when Jenkins authentication is on

1.1.1
=====================
* Support for Merge Requests from forked repositories
* Selective triggering (Push events and/or Merge Request events)

1.0.8
=====================
* Fix images not showing up. (Jotschi)
* Supprting nested groups with CloudBees Plugin (FrantaM)

1.0.7
=====================
* Initial Release

1.0.2 - 1.0.6
=====================
* Trying to figure out releases

1.0.1
=====================
* Changed Parameter names that were used by default by jenkins.

1.0
=====================
* Initial Release
