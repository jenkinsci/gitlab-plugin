# Table of Contents
- [Introduction](#introduction)
- [User support](#user-support)
- [Supported GitLab versions](#supported-gitLab-versions)
- [Supported GitLabCI Functions](#supported-gitlabci-functions)
- [Configuring access to GitLab](#configuring-access-to-gitlab)
    - [Jenkins Job Configuration](#jenkins-job-configuration)
    - [Gitlab Configuration (7.14.x)](#gitlab-configuration-714x)
    - [Gitlab Configuration (>= 8.1)](#gitlab-configuration--81)
    - [Forked repositories](#forked-repositories)
- [Branch filtering](#branch-filtering)
- [Build Tags](#build-tags)
- [Parameterized builds](#parameterized-builds)
- [Help Needed](#help-needed)
- [Quick test environment setup using Docker](#quick-test-environment-setup-using-docker)
    - [Access GitLab](#access-gitlab)
    - [Access Jenkins](#access-jenkins)
- [Release Workflow](#release-workflow)

# Introduction

This plugin allows GitLab to trigger builds in Jenkins after code is pushed and/or after a merge request is created.

# User support

If you have a problem or question about using the plugin, please create an issue in the GitHub project. You can also try chatting with us in #gitlab-plugin on the Freenode IRC network.

# Supported GitLab versions

* 7.14.x where it emulates Jenkins as a GitLabCI Web Service
* 8.1.x and newer via the new commit status API that supports with external CI services like Jenkins

**Note:** GitLab version **8.0.x** is **not** supported! In this version, GitLab folded the GitLabCI functionality into core GitLab, and in doing so broke the ability for the plugin to give build status to GitLab. Jenkins build status will never work with GitLab 8.0.x!

# Supported GitLabCI Functions

* `/project/PROJECT_NAME/builds/COMMIT_SHA1/status.json` (used for Merge Request pages) returns build result for Merge Request build with `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME/builds/status.png?ref=BRANCH_NAME` returns build status icon for latest build for `BRANCH_NAME`
* `/project/PROJECT_NAME/builds/status.png?sha1=COMMIT_SHA1` returns build status icon for latest build for `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME/builds/COMMIT_SHA1` redirects to build page of the last build containing `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME/commits/COMMIT_SHA1` redirects to build page of the last build containing `COMMIT_SHA1` as last commit
* `/project/PROJECT_NAME?ref=BRANCH_NAME` redirects to build page of the last build for `BRANCH_NAME`
* `/project/PROJECT_NAME` triggers a build, type (Merge Request or Push) depending on payload

# Configuring access to GitLab

Optionally, the plugin communicates with the GitLab server in order to fetch additional information. At this moment, this information is limited to fetching the source project of a Merge Request, in order to support merging from forked repositories.

To enable this functionality, a user should be set up on GitLab, with GitLab 'Developer' permissions, to access the repository. On the global configuration screen, supply the gitlab host url ``http://your.gitlab.server`` and the API token of the user of choice.

## Jenkins Job Configuration
* Create a new job by going to *New Job*
* Set the _Project Name_ to whatever you like
* In the *Source Code Management* section:
    * Click *Git*
    * Enter your *Repository URL* (e.g.: ``git@your.gitlab.server:group/repo_name.git``)
      * In the Advanced settings, set its *Name* to ``origin``
    * To be able to merge from forked repositories:  <br/>**Note:** this requires [configuring communication to the GitLab server](#configuring-access-to-gitlab)
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

## Gitlab Configuration (7.14.x)
* In GitLab go to your repository's project *Settings*
    * Click on *Services*
    * Click on *GitLab CI*
        * Check the *Active* checkbox 
        * For *Token* put any random string (This is not yet functioning)
        * For *Project URL* put ``http://JENKINS_URL/project/PROJECT_NAME``
        * Click *Save*
    * Click on *Web Hooks*
        * Add a Web Hook for *Merge Request Events* to ``http://JENKINS_URL/project/PROJECT_NAME`` <br/>
        **Note:** GitLab for some reason does not send a merge request event with the GitLab Service.

## Gitlab Configuration (>= 8.1)
* In GitLab go to you primary repository's project *Settings*
    * Click on *Web Hooks*
        * Add a Web Hook for *Merge Request Events* and *Push Events* to ``http://JENKINS_URL/project/PROJECT_NAME`` <br/>

If you plan to use forked repositories, you will need to enable the GitLab CI integration on **each fork**.
* Go to the Settings page in each developer's fork
* Click on *Services*
    * Click on *Web Hooks*
        * Add a Web Hook for *Merge Request Events* and *Push Events* to ``http://JENKINS_URL/project/PROJECT_NAME`` <br/>
        **Note:** You do not need to select any "Trigger Events" as the Web Hook for Merge Request Events will alert Jenkins.

* GitLab 8.1 has implemented a commit status api. To enable this check the ``Use GitLab CI features`` under the project settings.
* Configure access to GitLab as described above in "Configure access to GitLab" (the account needs at least developer permissions to post commit statuses)

## Forked repositories
If you plan to use forked repositories, you will need to enable the GitLab CI integration on **each fork**.
* Go to the Settings page in each developer's fork
* Click on *Services*
   * Click on *GitLab CI*
      * Check the *Active* checkbox 
      * For *Token* put any random string (This is not yet functioning)
      * For *Project URL* put ``http://JENKINS_URL/project/PROJECT_NAME``
      * Click *Save* <br />
      **Note:** You do not need to select any "Trigger Events" as the Web Hook for Merge Request Events will alert Jenkins.

In addition, you will need to make sure that the Git plugin has an appropriate setting for user.name and user.email in the global Jenkins configuration. This is good practice generally, but is required for forked repos to work.

1. Click on Manage Jenkins, then Configure System
2. Under the Git Plugin section, set something for 'Global Config user.name Value' and 'Global Config user.email Value'

# Branch filtering

Triggers from push events may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, a list of all branches on the remote repository is displayed under ``Build when a change is pushed to GitLab.``. It is possible to select multiple branches by holding Ctrl and clicking. 

This functionality requires accessing the GitLab server (see [above](#configuring-access-to-gitlab)) and for the time being also a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to select the allowed branches. For Workflow jobs, the configuration must be saved *and* the job must be run once before the list is populated. For existing projects, all branches are allowed to push by default.

# Build Tags

In order to build when a new tag is pushed:
* In the ``GitLab server`` add ``Tag push events`` to the ``Web Hook``
* In the ``Jenkins`` under the ``Source Code Management`` section:
    * select ``Advance...`` and add  ``+refs/tags/*:refs/remotes/origin/tags/*`` as ``Refspec``
    * you can also use ``Branch Specifier`` to specify which tag need to be built (exampple ``refs/tags/${TAGNAME}``)

# Parameterized builds

You can trigger a job a manually by clicking ``This build is parameterized`` and adding the relevant build parameters.
These include:

* gitlabSourceBranch
* gitlabTargetBranch
* gitlabSourceRepoURL
* gitlabSourceRepoName
* gitlabBranch (This is optional and can be used in shell scripts for the branch being built by the push request)
* gitlabActionType (This is optional and can be used in shell scripts or other plugins to change the build behaviour. Possible values are PUSH or MERGE)
* gitlabMergeRequestTitle
* gitlabMergeRequestId
* gitlabMergeRequestAssignee
* gitlabUserName
* gitlabUserEmail

# Help Needed

* `/projects/` - seems to be already used by Jenkins, A way to use this path would be awesome
* `?token=XYZ` - Can not find a way to include a token parameter on an AbstractProject to security check without an extra plugin configuration
* `/PROJECT_NAME/`  should really be /PROJECT_ID_NUMBER/ - Can not find a project id number on an AbstractProject to use here instead.

# Contributing to the Plugin

Plugin source code is hosted on [Github](https://github.com/jenkinsci/gitlab-plugin).
New feature proposals and bug fix proposals should be submitted as
[Github pull requests](https://help.github.com/articles/creating-a-pull-request).
Fork the repository on Github, prepare your change on your forked
copy, and submit a pull request (see [here](https://github.com/jenkinsci/gitlab-plugin/pulls) for open pull requests). Your pull request will be evaluated by the [Cloudbees Jenkins job](https://jenkins.ci.cloudbees.com/job/plugins/job/gitlab-plugin/).

If you are adding new features please make sure that they support the Jenkins Workflow Plugin.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md) for some information.

Before submitting your change make sure that:
* your changes work with the oldest and latest supported GitLab version
* new features are provided with tests
* refactored code is provided with regression tests
* the code formatting follows the plugin standard
* imports are organised
* you updated the help docs
* you updated the README
* you have used findbugs to see if you haven't introduced any new warnings.

# Quick test environment setup using Docker

In order to test the plugin on different versions of `GitLab` and `Jenkins` you may want to use `Docker` containers.

A example docker-compose file is available at `gitlab-plugin/src/docker` which allows to set up instances of the latest `GitLab` and `Jenkins` versions.

To start the containers, run below command from the `docker` folder:

```bash
docker-compose up -d
```

## Access GitLab

To access `GitLab`, point your browser to `http://localhost:10080` and login using the default username and password:

* username: **root**
* password: **5iveL!fe**

For more information on the supported `GitLab` versions and how to configure the containers, visit Sameer Naik's github page at https://github.com/sameersbn/docker-gitlab.

## Access Jenkins

To see `Jenkins`, point your browser to `http://localhost:8080`.

For more information on the supported `Jenkins` tags and how to configure the containers, visit https://hub.docker.com/r/library/jenkins.

# Release Workflow

GitLab-Plugin admins should adhere to the following rules when releasing a new plugin version:

* Ensure codestyle conformity
* Run unit tests
* Run manual tests on both, oldest and latest GitLab versions
* Update documentation
* Create change log
* Create release tag
* Create release notes (on github)
