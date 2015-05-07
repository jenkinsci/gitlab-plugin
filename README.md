gitlab-plugin
=====================

This plugin emulates Jenkins as a GitlabCI Web Service to be used with GitlabHQ.

[![Build Status](https://travis-ci.org/DABSquared/gitlab-plugin.svg?branch=master)](https://travis-ci.org/DABSquared/gitlab-plugin) 
[![Gitter chat](https://badges.gitter.im/DABSquared/gitlab-plugin.png)](https://gitter.im/DABSquared/gitlab-plugin)


Current Supported GitLabCI Functions
=====================
* `/project/PROJECT_NAME/builds/COMMIT_SHA1/status.json` (used for Merge Request pages) returns build result for Merge Request build with `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME/builds/status.png?ref=BRANCH_NAME` returns build status icon for latest build for `BRANCH_NAME`
* `/project/PROJECT_NAME/builds/status.png?sha1=COMMIT_SHA1` returns build status icon for latest build for `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME/builds/COMMIT_SHA1` redirects to build page of the last build containing `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME/commits/COMMIT_SHA1` redirects to build page of the last build containing `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME?ref=BRANCH_NAME` redirects to build page of the last build for `BRANCH_NAME`
* `/project/PROJECT_NAME` triggers a build, type (Merge Request or Push) depending on payload

Major Help Needed
=====================
I would like this project to be able to handle building merge requests and regular pushes. In order to do this I need a way to configure the git plugin via code to merge two branches together before a build. Much like the RevisionParameterAction.java in the git plugin, we need a class that takes to branches, a source and a target, and can be passed as a build action. I have started an issue for the Git plugin here: https://issues.jenkins-ci.org/browse/JENKINS-23362 If you know of a way to do this please PM on twitter at @bass_rock. All the other necessary code exists in this repo and works.

Configuring access to Gitlab
=======================================

Optionally, the plugin communicates with the Gitlab server in order to fetch additional information. At this moment, this information is limited to fetching the source project of a Merge Request, in order to support merging from forked repositories. 

To enable this functionality, a user should be set up on Gitlab, which adequate permissions to access the repository. On the global configuration screen, supply the gitlab host url ``http://your.gitlab.server`` and the API token of the user of choice.

Using it With A Job
=====================
* Create a new job by going to *New Job*
* Set the _Project Name_ to whatever you like
* If you have the GitHub plugin installed, feel free to specify the ``GitHub Project`` url as the url for the Gitlab project.
* In the *Source Code Management* section:
    * Click *Git*
    * Enter your *Repository URL* (e.g.: ``git@your.gitlab.server:group/repo_name.git``)
      * In the Advanced settings, set its *Name* to ``origin``
    * To be able to merge from forked repositories:  <br/>**Note:** this requires [configuring communication to the Gitlab server](#configuring-access-to-gitlab))
      * Add a second repository with:
        * *URL*: ``${gitlabSourceRepoURL}`` 
        * *Name* (in Advanced): ``${gitlabSourceRepoName}``
    * In *Branch Specifier* enter:
      * For single-repository setups: ``origin/${gitlabSourceBranch}``
      * For forked repository setups: ``${gitlabSourceRepoName}/${gitlabSourceBranch}``
    * In *Additional Behaviours*:
        * Click the *Add* drop-down button.
        * Select *Merge before build* from the drop-down.
        * Set *Name of the repository" to ``origin`` 
        * Set *Branch to merge* as ``${gitlabTargetBranch}``
* In the *Build Triggers* section:
    * Check the ``Build when a change is pushed to GitLab.``
    * Use the check boxes to trigger builds on Push and/or Merge Request events
    * Optionally enable building open merge requests again after a push to the source branch.
* Configure any other pre build, build or post build actions as necessary
* Click *Save* to preserve your changes in Jenkins.

### GitLab Configuration
* In GitLab go to you primary repository's project *Settings*
    * Click on *Services*
    * Click on *GitLab CI*
        * Check the *Active* checkbox 
        * For *Token* put any random string (This is not yet functioning)
        * For *Project URL* put ``http://JENKINS_URL/project/PROJECT_NAME``
        * Click *Save*
    * Click on *Web Hooks*
        * Add a Web Hook for *Merge Request Events* to ``http://JENKINS_URL/project/PROJECT_NAME`` <br/>
        **Note:** GitLab for some reason does not send a merge request event with the GitLab Service.

If you plan to use forked repositories, you will need to enable the GitLab CI integration on **each fork**.
* Go to the Settings page in each developer's fork
* Click on *Services*
   * Click on *GitLab CI*
      * Check the *Active* checkbox 
      * For *Token* put any random string (This is not yet functioning)
      * For *Project URL* put ``http://JENKINS_URL/project/PROJECT_NAME``
      * Click *Save* <br />
      **Note:** You do not need to select any "Trigger Events" as the Web Hook for Merge Request Events will alert Jenkins.

Branch filtering
================

Triggers from push events may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, a list of all branches on the remote repository is displayed under ``Build when a change is pushed to GitLab.``. It is possible to select multiple branches by holding Ctrl and clicking. 

This functionality requires accessing the Gitlab server (see [above](#configuring-access-to-gitlab)) and for the time being also a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to select the allowed branches. For existing projects, all branches are allowed to push by default.

Build Tags
================

In order to build when a new tag is pushed:
* In the ``GitLab server`` add ``Tag push events`` to the ``Web Hook``
* In the ``Jenkins`` under the ``Source Code Management`` section:
    * select ``Advance...`` and add  ``+refs/tags/*:refs/remotes/origin/tags/*`` as ``Refspec``
    * you can also use ``Branch Specifier`` to specify which tag need to be built (exampple ``refs/tags/${TAGNAME}``)

Parameterized builds
====================

You can trigger a job a manually by clicking ``This build is parameterized`` and adding the relevant build parameters.
These include:

* gitlabSourceBranch
* gitlabTargetBranch
* gitlabSourceRepoURL
* gitlabSourceRepoName
* gitlabBranch (This is optional and can be used in shell scripts for the branch being built by the push request)


Help Needed
=====================

* `/projects/` - seems to be already used by Jenkins, A way to use this path would be awesome
* `?token=XYZ` - Can not find a way to include a token parameter on an AbstractProject to security check without an extra plugin configuration
* `/PROJECT_NAME/`  should really be /PROJECT_ID_NUMBER/ - Can not find a project id number on an AbstractProject to use here instead.


Known Issues
=====================
* GitLab CI Merge Status pages says pending when there is no build scheduled, or the status is unknown. This is because I coded a workaround until this bug gets resolved: https://github.com/gitlabhq/gitlabhq/issues/7047


Contributing
=====================

1. Fork it ( https://github.com/[my-github-username]/gitlab-plugin/fork )
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

Contributors
=====================

* @bass_rock, base ground work, primary developer.
* @DABSquared, company sponsoring development.
* @xaniasd

Parts of this code inspired by https://github.com/timols/jenkins-gitlab-merge-request-builder-plugin
