# Table of Contents
- [Introduction](#introduction)
- [User support](#user-support)
- [Known bugs/issues](#known-bugsissues)
- [Defined variables](#defined-variables)
- [Global plugin configuration](#global-plugin-configuration)
  - [GitLab-to-Jenkins auth](#gitlab-to-jenkins-authentication)
  - [Jenkins-to-GitLab auth](#jenkins-to-gitlab-authentication)
 - [Jenkins Job Configuration](#jenkins-job-configuration)
   - [Git configuration](#git-configuration)
     - [Freestyle jobs](#freestyle-jobs)
     - [Pipeline jobs](#pipeline-jobs)
     - [Pipeline Multibranch jobs](#pipeline-multibranch-jobs)
   - [Job trigger configuration](#job-trigger-configuration)
     - [Freestyle and Pipeline jobs](#freestyle-and-pipeline-jobs)
     - [Pipeline Multibranch jobs](#pipeline-multibranch-jobs-1)
   - [Build status configuration](#build-status-configuration)
     - [Freestyle jobs](#freestyle-jobs-1)
     - [Scripted Pipeline jobs](#scripted-pipeline-jobs)
     - [Declarative Pipeline jobs](#declarative-pipeline-jobs)
     - [Matrix jobs](#matrixmulti-configuration-jobs)
- [Advanced features](#advanced-features)
  - [Branch filtering](#branch-filtering)
  - [Build when tags are pushed](#build-when-tags-are-pushed)
  - [Add a note to merge requests](#add-a-note-to-merge-requests)
  - [Parameterized builds](#parameterized-builds)
- [Contributing to the Plugin](#contributing-to-the-plugin)
- [Testing With Docker](#testing-with-docker)
- [Release Workflow](#release-workflow)

# Introduction

This plugin allows GitLab to trigger builds in Jenkins when code is committed or merge requests are opened/updated. It can also send build status back to GitLab.

### Seeking maintainers

Most of the current maintainers of the plugin are no longer using GitLab on a daily basis, and therefore have less time to contribute to it. If you are a regular user and would like to help out, please consider volunteering as a maintainer. There are verified bugs that need fixes, open PRs that need review, and feature requests that range from simple to complex. If you are interested in contributing, contact Owen (email address in git log) for additional access.

# User support

If you have a problem or question about using the plugin, please make sure you are using the latest version. Then create an issue in the GitHub project if necessary. New issues should include the following:
* GitLab plugin version (e.g. 1.5.2)
* GitLab version (e.g. 10.5.1)
* Jenkins version (e.g. 2.111)
* Relevant log output from the plugin (see below for instructions on capturing this)

Version 1.2.0 of the plugin introduced improved logging for debugging purposes. To enable it:

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'Gitlab plugin' or whatever you want for the name
4. On the next page, enter 'com.dabsquared.gitlabjenkins' for Logger, set log level to FINEST, and save
5. Then click on your Gitlab plugin log, click 'Clear this log' if necessary, and then use GitLab to trigger some actions
6. Refresh the log page and you should see output

You can also try chatting with us in the #gitlab-plugin channel on the Freenode IRC network.

# Known bugs/issues

This is not an exhaustive list of issues, but rather a place for us to note significant bugs that may impact your use of the plugin in certain circumstances. For most things, please search the [Issues](https://github.com/jenkinsci/gitlab-plugin/issues) section and open a new one if you don't find anything.
* [#272](https://github.com/jenkinsci/gitlab-plugin/issues/272) - Plugin version 1.2.0+ does not work with GitLab Enterprise Edition < 8.8.3. Subsequent versions work fine.
* Jenkins versions 1.651.2 and 2.3 removed the ability of plugins to set arbitrary job parameters that are not specifically defined in each job's configuration. This was an important security update, but it has broken compatibility with some plugins, including ours. See [here](https://jenkins.io/blog/2016/05/11/security-update/) for more information and workarounds if you are finding parameters unset or empty that you expect to have values.
* [#473](https://github.com/jenkinsci/gitlab-plugin/issues/473) - When upgrading from plugin versions older than 1.2.0, you must upgrade to that version first, and then to the latest version. Otherwise, you will get a NullPointerException in com.cloudbees.plugins.credentials.matchers.IdMatcher after you upgrade. See the linked issue for specific instructions.
* [#608](https://github.com/jenkinsci/gitlab-plugin/issues/608) - GitLab 9.5.0 - 9.5.4 has a bug that causes the "Test Webhook" function to fail when it sends a test to Jenkins. This was fixed in 9.5.5.

# Defined variables

When GitLab triggers a build via the plugin, various environment variables are set based on the JSON payload that GitLab sends. You can use these throughout your job configuration. The available variables are:

```
gitlabBranch
gitlabSourceBranch
gitlabActionType
gitlabUserName
gitlabUserEmail
gitlabSourceRepoHomepage
gitlabSourceRepoName
gitlabSourceNamespace
gitlabSourceRepoURL
gitlabSourceRepoSshUrl
gitlabSourceRepoHttpUrl
gitlabMergeRequestTitle
gitlabMergeRequestDescription
gitlabMergeRequestId
gitlabMergeRequestIid
gitlabMergeRequestState
gitlabMergedByUser
gitlabMergeRequestAssignee
gitlabMergeRequestLastCommit
gitlabMergeRequestTargetProjectId
gitlabTargetBranch
gitlabTargetRepoName
gitlabTargetNamespace
gitlabTargetRepoSshUrl
gitlabTargetRepoHttpUrl
gitlabBefore
gitlabAfter
gitlabTriggerPhrase
```

 **NOTE:** These variables are not available in Pipeline Multibranch jobs. 

# Global plugin configuration
## GitLab-to-Jenkins authentication
By default the plugin will require authentication to be set up for the connection from GitLab to Jenkins, in order to prevent unauthorized persons from being able to trigger jobs. 

### Configuring global authentication
1. Create a user in Jenkins which has, at a minimum, Job/Build permissions
2. Log in as that user (this is required even if you are a Jenkins admin user), then click on the user's name in the top right corner of the page
3. Click 'Configure,' then 'Show API Token...', and note/copy the User ID and API Token
4. In GitLab, when you create webhooks to trigger Jenkins jobs, use this format for the URL and do not enter anything for 'Secret Token': `http://USERID:APITOKEN@JENKINS_URL/project/YOUR_JOB`
5. After you add the webhook, click the 'Test' button, and it should succeed

### Configuring per-project authentication

If you want to create separate authentication credentials for each Jenkins job:
1. In the configuration of your Jenkins job, in the GitLab configuration section, click 'Advanced'
2. Click the 'Generate' button under the 'Secret Token' field
3. Copy the resulting token, and save the job configuration
4. In GitLab, create a webhook for your project, enter the trigger URL (e.g. `http://JENKINS_URL/project/YOUR_JOB`) and paste the token in the Secret Token field
5. After you add the webhook, click the 'Test' button, and it should succeed

### Disabling authentication

If you want to disable this authentication (not recommended):
1. In Jenkins, go to Manage Jenkins -> Configure System
2. Scroll down to the section labeled 'GitLab'
3. Uncheck "Enable authentication for '/project' end-point" - you will now be able to trigger Jenkins jobs from GitLab without needing authentication

## Jenkins-to-GitLab authentication
**PLEASE NOTE:** This auth configuration is only used for accessing the GitLab API for sending build status to GitLab. It is **not** used for cloning git repos. The credentials for cloning (usually SSH credentials) should be configuring separately, in the git plugin.

This plugin can be configured to send build status messages to GitLab, which show up in the GitLab Merge Request UI. To enable this functionality: 
1. Create a new user in GitLab
2. Give this user 'Developer' permissions on each repo you want Jenkins to send build status to
3. Log in or 'Impersonate' that user in GitLab, click the user's icon/avatar and choose Settings
4. Click on 'Access Tokens'
5. Create a token named e.g. 'jenkins' with 'api' scope; expiration is optional
6. Copy the token immediately, it cannot be accessed after you leave this page
7. On the Global Configuration page in Jenkins, in the GitLab configuration section, supply the GitLab host URL, e.g. `http://your.gitlab.server` 
8. Click the 'Add' button to add a credential, choose 'GitLab API token' as the kind of credential, and paste your GitLab user's API key into the 'API token' field
9. Click the 'Test Connection' button; it should succeed

# Jenkins Job Configuration

There are two aspects of your Jenkins job that you may want to modify when using GitLab to trigger jobs. The first is the Git configuration, where Jenkins clones your git repo. The GitLab Plugin will set some environment variables when GitLab triggers a build, and you can use those to control what code is cloned from Git. The second is the configuration for sending the build status back to GitLab, where it will be visible in the commit and/or merge request UI.

## Git configuration 
### Freestyle jobs
In the *Source Code Management* section:

1. Click *Git*
2. Enter your *Repository URL*, such as ``git@your.gitlab.server:gitlab_group/gitlab_project.git``
    1. In the *Advanced* settings, set *Name* to ``origin`` and *Refspec* to ``+refs/heads/*:refs/remotes/origin/* +refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*``
3. In *Branch Specifier* enter:
    1. For single-repository workflows: ``origin/${gitlabSourceBranch}``
    2. For forked repository workflows: ``merge-requests/${gitlabMergeRequestIid}``
4. In *Additional Behaviours*:
    1. Click the *Add* drop-down button
    2. Select *Merge before build* from the drop-down
    3. Set *Name of repository* to ``origin``
    4. Set *Branch to merge* as ``${gitlabTargetBranch}``

**Note:** Since version **1.2.0** the *gitlab-plugin* sets the gitlab hook values through *environment variables* instead of *build parameters*. To set default values, consult [EnvInject Plugin](https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin).

### Pipeline jobs
* A Jenkins Pipeline bug will prevent the Git clone from working when you use a Pipeline script from SCM. It works if you use the Jenkins job config UI to edit the script. There is a workaround mentioned here: https://issues.jenkins-ci.org/browse/JENKINS-33719

* Use the Snippet generator, General SCM step, to generate sample Groovy code for the git checkout/merge etc.
* Example that performs merge before build:
```
checkout changelog: true, poll: true, scm: [
    $class: 'GitSCM',
    branches: [[name: "origin/${env.gitlabSourceBranch}"]],
    doGenerateSubmoduleConfigurations: false,
    extensions: [[$class: 'PreBuildMerge', options: [fastForwardMode: 'FF', mergeRemote: 'origin', mergeStrategy: 'default', mergeTarget: "${env.gitlabTargetBranch}"]]],
    submoduleCfg: [],
    userRemoteConfigs: [[name: 'origin', url: 'git@gitlab.example.com:foo/testrepo.git']]
    ]
```

### Pipeline Multibranch jobs
**Note:** There is no way to pass external data from GitLab to a Pipeline Multibranch job, so the GitLab environment variables are **not populated** for this job type. GitLab will just trigger branch indexing for the Jenkins project, and Jenkins will build branches accordingly without needing e.g. the git branch env var. **Due to this the plugin just listens for GitLab Push Hooks for multibranch pipeline jobs; Merge Request hooks are ignored.**

1. Click **Add source**
2. Select **Git**
3. Enter your *Repository URL* (e.g.: ``git@your.gitlab.server:group/repo_name.git``)

Example `Jenkinsfile` for multibranch pipeline jobs
```
// Reference the GitLab connection name from your Jenkins Global configuration (http://JENKINS_URL/configure, GitLab section)
properties([gitLabConnection('your-gitlab-connection-name')])

node {
  checkout scm // Jenkins will clone the appropriate git branch, no env vars needed

  // Further build steps happen here
}
```

## Job trigger configuration
### Freestyle and Pipeline jobs
1. In the *Build Triggers* section:
    * Select *Build when a change is pushed to GitLab*
    * Make a note of the *GitLab CI Service URL* appearing on the same line with *Build when a change is
      pushed to GitLab*.  You will later use this URL to define a GitLab web hook.
    * Use the check boxes to trigger builds on *Push Events* and/or *Created Merge Request Events* and/or *Accepted Merge Request Events* and/or *Closed Merge Request Events*
    * Optionally use *Rebuild open Merge Requests* to enable re-building open merge requests after a
      push to the source branch
    * If you selected *Rebuild open Merge Requests* other than *None*, check *Comments*, and specify the
      *Comment for triggering a build*.  A new build will be triggered when this phrase appears in a
      commit comment.  In addition to a literal phrase, you can also specify a Java regular expression.
    * You can use *Build on successful pipeline events* to trigger on a successful pipeline run in Gitlab. Note that 
      this build trigger will only trigger a build if the commit is not already built and does not set the Gitlab status.
      Otherwise you might end up in a loop.
2. Configure any other pre build, build or post build actions as necessary
3. Click *Save* to preserve your changes in Jenkins.

### Pipeline Multibranch jobs
Unlike other job types, there is no 'Trigger' setting required for a Multibranch job configuration; just create a webhook in GitLab for push requests which points to ``http://JENKINS_URL/project/PROJECT_NAME`` or ``http://JENKINS_URL/project/FOLDER/PROJECT_NAME`` if the project in inside a folder in Jenkins. When GitLab POSTs to this URL, it will trigger branch indexing for the Jenkins project, and Jenkins will handle starting any builds necessary.

## Build status configuration
You can optionally have your Jenkins jobs send their build status back to GitLab, where it will be displayed in the commit or merge request UI as appropriate. 

### Freestyle jobs
Freestyle jobs can only send build status after the build steps are complete. To do this, choose 'Publish build status to GitLab' from the available 'Post-build actions' in your Jenkins job config. Also make sure you have chosen the appropriate GitLab instance from the 'GitLab connection' dropdown menu, if you have more than one.

### Scripted Pipeline jobs
**NOTE:** If you use Pipeline global libraries, or if you clone your project's Jenkinsfile from a repo different from the one that contains the relevant source code, you need to be careful about when you send project status. In short, make sure you put your `gitlabCommitStatus` or other similar steps *after* the SCM step that clones your project's source. Otherwise, you may get HTTP 400 errors, or you may find build status being sent to the wrong repo.

* For Pipeline jobs surround your build steps with the `gitlabCommitStatus` step like this:
    ```
    node() {
        stage('Checkout') { checkout <your-scm-config> }

        gitlabCommitStatus {
           // The result of steps within this block is what will be sent to GitLab
           sh 'mvn install'
        }
    }
    ```
* Or use the `updateGitlabCommitStatus` step to use a custom value for updating the commit status. You could use try/catch blocks or other logic to send fine-grained status of the build to GitLab. Valid statuses are defined by GitLab and documented here: https://docs.gitlab.com/ce/api/pipelines.html
    ```
    node() {
        stage('Checkout') { checkout <your-scm-config> }

        updateGitlabCommitStatus name: 'build', state: 'pending'
        // Build steps
        
        updateGitlabCommitStatus name: 'build', state: 'success'
    }
    ```
* Or you can mark several build stages as pending in GitLab, using the `gitlabBuilds` step:
    ```
    node() {
        stage('Checkout') { checkout <your-scm-config> }

        gitlabBuilds(builds: ["build", "test"]) {
            stage("build") {
              gitlabCommitStatus("build") {
                  // your build steps
              }
            }

            stage("test") {
              gitlabCommitStatus("test") {
                  // your test steps
              }
            }
        }
    }
    ```

### Declarative Pipeline jobs
The example below configures the GitLab connection and job triggers. It also sends build status back to GitLab.

```
pipeline {
    agent any
    post {
      failure {
        updateGitlabCommitStatus name: 'build', state: 'failed'
      }
      success {
        updateGitlabCommitStatus name: 'build', state: 'success'
      }
    }
    options {
      gitLabConnection('your-gitlab-connection-name')
    }
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
    }
    stages {
      stage("build") {
        steps {
          echo "hello world"
        }
      }
    }
   [...]
}
```

If:
1. You use the *"Merge When Pipeline Succeeds"* option for Merge Requests in GitLab, and 
2. Your Declarative Pipeline jobs have more than one stage, and
3. You use a `gitlabCommitStatus` step *in each stage* to send status to GitLab...

Then:
You will need to define those stages in an `options` block. Otherwise, when and if the first stage passes, GitLab will merge the change. For example, if you have three stages named build, test, and deploy:

```
    options {
      gitLabConnection('your-gitlab-connection-name')
      gitlabBuilds(builds: ['build', 'test', 'deploy'])
    }
```

If you want to configure any of the optional job triggers that the plugin supports in a Declarative build, use a `triggers` block. The full list of configurable trigger options is as follows:

```
triggers {
    gitlab(
      triggerOnPush: false,
      triggerOnMergeRequest: true, triggerOpenMergeRequestOnPush: "never",
      triggerOnNoteRequest: true,
      noteRegex: "Jenkins please retry a build",
      skipWorkInProgressMergeRequest: true,
      ciSkip: false,
      setBuildDescription: true,
      addNoteOnMergeRequest: true,
      addCiMessage: true,
      addVoteOnMergeRequest: true,
      acceptMergeRequestOnSuccess: false,
      branchFilterType: "NameBasedFilter",
      includeBranchesSpec: "release/qat",
      excludeBranchesSpec: "",
      secretToken: "abcdefghijklmnopqrstuvwxyz0123456789ABCDEF")
}
```

### Matrix/Multi-configuration jobs

This plugin can be used with Matrix/Multi-configuration jobs together with the [Flexible Publish](https://plugins.jenkins.io/flexible-publish) plugin which allows you to run publishers after all axis jobs are done. Configure the *Post-build Actions* as follows:

1. Add a *Flexible publish* action
2. In the *Flexible publish* section:
      1. *Add conditional action*
      2. In the *Conditional action* section:
          1. Set *Run?* to *Never*
          2. Select *Condition for Matrix Aggregation*
          3. Set *Run on Parent?* to *Always*
          4. Add GitLab actions as required

# Advanced features
## Branch filtering
Triggers may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, when you configure the GitLab trigger, you can choose 'Filter branches by name' or 'Filter branches by regex.' Filter by name takes comma-separated lists of branch names to include and/or exclude from triggering a build. Filter by regex takes a Java regular expression to include and/or exclude.

**Note:** This functionality requires access to GitLab and a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to add branch filters. For Pipeline jobs, the configuration must be saved *and* the job must be run once before the list is populated.

## Build when tags are pushed
In order to build when a new tag is pushed:
1. In the GitLab webhook configuration, add 'Tag push events'
2. In the job configuration under 'Source code management':
    1. Select 'Advanced...' and add '+refs/tags/*:refs/remotes/origin/tags/*' as the Refspec
    2. You can also use 'Branch Specifier' to specify which tag need to be built (example 'refs/tags/${TAGNAME}')

## Add a note to merge requests
To add a note to GitLab merge requests after the build completes, select 'Add note with build status on GitLab merge requests' from the optional Post-build actions. Optionally, click the 'Advanced' button to customize the content of the note depending on the build result.

## Parameterized builds
You can trigger a job a manually by clicking 'This build is parameterized' in the job configuration and adding the any of the relevant build parameters. See the [defined parameters](#defined-parameters) list. If you only care about jobs being triggered from GitLab webhooks, this step is unnecessary.

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

# Testing With Docker

See https://github.com/jenkinsci/gitlab-plugin/tree/master/src/docker/README.md

# Release Workflow

To perform a full plugin release, maintainers can run ``mvn release:prepare release:perform`` To release a snapshot, e.g. with a bug fix for users to test, just run ``mvn deploy``
