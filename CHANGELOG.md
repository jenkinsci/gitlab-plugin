ChangeLog
=====================

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
