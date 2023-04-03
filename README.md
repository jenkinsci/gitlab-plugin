# GitLab Plugin

## Table of Contents
- [Introduction](#introduction)
- [User support](#user-support)
- [Known bugs/issues](#known-bugsissues)
- [Defined variables](#defined-variables)
- [Global plugin configuration](#global-plugin-configuration)
  - [GitLab-to-Jenkins auth](#gitlab-to-jenkins-authentication)
  - [Jenkins-to-GitLab auth](#jenkins-to-gitlab-authentication)
  - [Jenkins Job Configuration](#jenkins-job-configuration)
   - [Parameter configuration](#parameter-configuration)
   - [Git configuration](#git-configuration)
     - [Freestyle jobs](#freestyle-jobs)
     - [Pipeline jobs](#pipeline-jobs)
     - [Pipeline Multibranch jobs](#pipeline-multibranch-jobs)
   - [Job trigger configuration](#job-trigger-configuration)
     - [Webhook URL](#webhook-url)
     - [Freestyle and Pipeline jobs](#freestyle-and-pipeline-jobs)
     - [Pipeline Multibranch jobs](#pipeline-multibranch-jobs-1)
     - [Multibranch Pipeline jobs with Job DSL](#multibranch-pipeline-jobs-with-job-dsl)
   - [Build status configuration](#build-status-configuration)
     - [Freestyle jobs](#freestyle-jobs-1)
     - [Scripted Pipeline jobs](#scripted-pipeline-jobs)
     - [Declarative Pipeline jobs](#declarative-pipeline-jobs)
     - [Matrix jobs](#matrixmulti-configuration-jobs)
  - [See also](#see-also)
- [Advanced features](#advanced-features)
  - [Branch filtering](#branch-filtering)
  - [Build when tags are pushed](#build-when-tags-are-pushed)
  - [Add a note to merge requests](#add-a-note-to-merge-requests)
  - [Accept merge request after build](#accept-merge-request)
  - [Notify specific project by a specific GitLab connection](#notify-specific-project-by-a-specific-gitlab-connection)
  - [Cancel pending builds on merge request update](#cancel-pending-builds-on-merge-request-update)
- [Compatibility](#compatibility)
- [Contributing to the Plugin](#contributing-to-the-plugin)
- [Testing With Docker](src/docker/README.md#quick-test-environment-setup-using-docker-for-linuxamd64)
- [Release Workflow](CONTRIBUTING.md#release-workflow)
- [Changelog](#changelog)

## Introduction

This plugin allows GitLab to trigger builds in Jenkins when code is committed or merge requests are opened/updated. It can also send build status back to GitLab.

## User support
#### Relationship with GitLab Inc.
This plugin is Open Source Software, developed on a volunteer basis by users of Jenkins and GitLab. It is not formally supported by either GitLab Inc. or CloudBees Inc.

#### Supported GitLab versions
GitLab performs a new major release about every six to nine months, and they are constantly fixing bugs and adding new features. As a result, we cannot support this plugin when used with GitLab versions older than N-2, where N is the current major release. At the time of this writing, the current stable release of GitLab is 11.1, so the oldest release supported by this plugin is 9.0.

#### Getting help
If you have a problem or question about using the plugin, please make sure you are using the latest version. Then create an issue in the GitHub project.

To enable debug logging in the plugin:

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'GitLab plugin' or whatever you want for the name
4. On the next page, enter 'com.dabsquared.gitlabjenkins' for Logger, set log level to FINEST, and save
5. Then click on your GitLab plugin log, click 'Clear this log' if necessary, and then use GitLab to trigger some actions
6. Refresh the log page and you should see output

## Known bugs/issues

This is not an exhaustive list of issues, but rather a place for us to note significant bugs that may impact your use of the plugin in certain circumstances. For most things, please search the [Issues](https://github.com/jenkinsci/gitlab-plugin/issues) section and open a new one if you don't find anything.
* [#272](https://github.com/jenkinsci/gitlab-plugin/issues/272) - Plugin version 1.2.0+ does not work with GitLab Enterprise Edition < 8.8.3. Subsequent versions work fine.
* Jenkins versions 1.651.2 and 2.3 removed the ability of plugins to set arbitrary job parameters that are not specifically defined in each job's configuration. This was an important security update, but it has broken compatibility with some plugins, including ours. See [here](https://jenkins.io/blog/2016/05/11/security-update/) for more information and workarounds if you are finding parameters unset or empty that you expect to have values.
* [#473](https://github.com/jenkinsci/gitlab-plugin/issues/473) - When upgrading from plugin versions older than 1.2.0, you must upgrade to that version first, and then to the latest version. Otherwise, you will get a NullPointerException in com.cloudbees.plugins.credentials.matchers.IdMatcher after you upgrade. See the linked issue for specific instructions.
* [#608](https://github.com/jenkinsci/gitlab-plugin/issues/608) - GitLab 9.5.0 - 9.5.4 has a bug that causes the "Test Webhook" function to fail when it sends a test to Jenkins. This was fixed in 9.5.5.
* [#730](https://github.com/jenkinsci/gitlab-plugin/issues/730) - GitLab 10.5.6 introduced an issue which can cause HTTP 500 errors when webhooks are triggered if the webhook is pointing to http://localhost or http://127.0.0.1. See the linked issue for a workaround.

## Report an Issue

Please report issues and enhancements through the [GitHub issue tracker](https://github.com/jenkinsci/gitlab-plugin/issues/new/choose).

## Defined variables

When GitLab triggers a build via the plugin, various environment variables are set based on the JSON payload that GitLab sends. You can use these throughout your job configuration. The available variables are:

```
gitlabBranch
gitlabSourceBranch
gitlabActionType
gitlabUserName
gitlabUserUsername
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

## Global plugin configuration
### GitLab-to-Jenkins authentication
By default the plugin will require authentication to be set up for the connection from GitLab to Jenkins, in order to prevent unauthorized persons from being able to trigger jobs.

#### Authentication Security

APITOKENS and other secrets MUST not be send over unsecure connections. So, all connections SHOULD use HTTPS.
> Note: Certificates are free and easy to manage with [LetsEncrypt](https://letsencrypt.org/).


#### Configuring global authentication
1. Create a user in Jenkins which has, at a minimum, Job/Build permissions
2. Log in as that user (this is required even if you are a Jenkins admin user), then click on the user's name in the top right corner of the page
3. Click 'Configure,' then 'Show API Token...', and note/copy the User ID and API Token
4. In GitLab, when you create webhooks to trigger Jenkins jobs, use this format for the URL and do not enter anything for 'Secret Token': `https://USERID:APITOKEN@JENKINS_URL/project/YOUR_JOB`
5. After you add the webhook, click the 'Test' button, and it should succeed

#### Configuring per-project authentication

If you want to create separate authentication credentials for each Jenkins job:
1. In the configuration of your Jenkins job, in the GitLab configuration section, click 'Advanced'
2. Click the 'Generate' button under the 'Secret Token' field
3. Copy the resulting token, and save the job configuration
4. In GitLab, create a webhook for your project, enter the trigger URL (e.g. `https://JENKINS_URL/project/YOUR_JOB`) and paste the token in the Secret Token field
5. After you add the webhook, click the 'Test' button, and it should succeed

#### Disabling authentication

If you want to disable this authentication (not recommended):
1. In Jenkins, go to Manage Jenkins -> Configure System
2. Scroll down to the section labeled 'GitLab'
3. Uncheck "Enable authentication for '/project' end-point" - you will now be able to trigger Jenkins jobs from GitLab without needing authentication

### Jenkins-to-GitLab authentication
**PLEASE NOTE:** This auth configuration is only used for accessing the GitLab API for sending build status to GitLab. It is **not** used for cloning git repos. The credentials for cloning (usually SSH credentials) should be configured separately, in the git plugin.

This plugin can be configured to send build status messages to GitLab, which show up in the GitLab Merge Request UI. To enable this functionality:
1. Create a new user in GitLab
2. Give this user 'Maintainer' permissions on each repo you want Jenkins to send build status to
3. Log in or 'Impersonate' that user in GitLab, click the user's icon/avatar and choose Settings
4. Click on 'Access Tokens'
5. Create a token named e.g. 'jenkins' with 'api' scope; expiration is optional
6. Copy the token immediately, it cannot be accessed after you leave this page
7. On the Global Configuration page in Jenkins, in the GitLab configuration section, supply the GitLab host URL, e.g. `https://your.gitlab.server`
8. Click the 'Add' button to add a credential, choose 'GitLab API token' as the kind of credential, and paste your GitLab user's API key into the 'API token' field
9. Click the 'Test Connection' button; it should succeed

## Jenkins Job Configuration

There are two aspects of your Jenkins job that you may want to modify when using GitLab to trigger jobs. The first is the Git configuration, where Jenkins clones your git repo. The GitLab Plugin will set some environment variables when GitLab triggers a build, and you can use those to control what code is cloned from Git. The second is the configuration for sending the build status back to GitLab, where it will be visible in the commit and/or merge request UI.

### Parameter configuration
**If you want to be able to run jobs both manually *and* automatically via GitLab webhooks, you will need to configure parameters for those jobs.** If you only want to trigger jobs from GitLab, you can skip this section.

Any GitLab parameters you create will always take precedence over the values that are sent by the webhook, unless you use the [EnvInject plugin](https://plugins.jenkins.io/envinject) to map the webhook values onto the job parameters. This is due to changes that were made to address [security vulnerabilities,](https://jenkins.io/security/advisory/2016-05-11/) with changes that landed in Jenkins 2.3.

In your job configuration, click 'This build is parameterized' and add any parameters you want to use. See the [defined variables](#defined-variables) list for options - your parameter names must match these .e.g `sourceBranch` and `targetBranch` in the example Groovy Script below. Then, having installed EnvInject, click 'Prepare an environment for the run' and check:
* Keep Jenkins Environment Variables
* Keep Jenkins Build Variables
* Override Build Parameters

In the Groovy Script field insert something similar to:

```groovy
def env = currentBuild.getEnvironment(currentListener)
def map = [:]

if (env.gitlabSourceBranch != null) {
  map['sourceBranch'] = env.gitlabSourceBranch
}

if (env.gitlabTargetBranch != null) {
  map['targetBranch'] = env.gitlabTargetBranch
}

return map
```

You can then reference these variables in your job config, e.g. as `${sourceBranch}`. You will need to update this code anytime you add or remove parameters.

Note: If you use the Groovy Sandbox, you might need to approve the script yourself or let an administrator approve the script in the Jenkins configuration.

### Git configuration
#### Freestyle jobs
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

#### Pipeline jobs
* A Jenkins Pipeline bug will prevent the Git clone from working when you use a Pipeline script from SCM. It works if you use the Jenkins job config UI to edit the script. There is a workaround mentioned here: https://issues.jenkins-ci.org/browse/JENKINS-33719

* Use the Snippet generator, General SCM step, to generate sample Groovy code for the git checkout/merge etc.
* Example that performs merge before build:
```groovy
checkout changelog: true, poll: true, scm: [
    $class: 'GitSCM',
    branches: [[name: "origin/${env.gitlabSourceBranch}"]],
    extensions: [[$class: 'PreBuildMerge', options: [fastForwardMode: 'FF', mergeRemote: 'origin', mergeStrategy: 'DEFAULT', mergeTarget: "${env.gitlabTargetBranch}"]]],
    userRemoteConfigs: [[name: 'origin', url: 'git@gitlab.example.com:foo/testrepo.git']]
    ]
```

#### Pipeline Multibranch jobs
**Note:** There is no way to pass external data from GitLab to a Pipeline Multibranch job, so the GitLab environment variables are **not populated** for this job type. GitLab will just trigger branch indexing for the Jenkins project, and Jenkins will build branches accordingly without needing e.g. the git branch env var. **Due to this, the plugin just listens for GitLab Push Hooks for multibranch pipeline jobs; merge Request hooks are ignored.**

1. Click **Add source**
2. Select **Git**
3. Enter your *Repository URL* (e.g.: ``git@your.gitlab.server:group/repo_name.git``)

Example `Jenkinsfile` for multibranch pipeline jobs:
```groovy
// Reference the GitLab connection name from your Jenkins Global configuration (https://JENKINS_URL/configure, GitLab section)
properties([gitLabConnection('your-gitlab-connection-name')])

node {
  checkout scm // Jenkins will clone the appropriate git branch, no env vars needed

  // Further build steps happen here
}
```

### Job trigger configuration
#### Webhook URL
When you configure the plugin to trigger your Jenkins job, by following the instructions below depending on job type, it will listen on a dedicated URL for JSON POSTs from GitLab's webhooks. That URL always takes the form ``https://JENKINS_URL/project/PROJECT_NAME``, or ``https://JENKINS_URL/project/FOLDER/PROJECT_NAME`` if the project is inside a folder in Jenkins. **You should not be using** ``https://JENKINS_URL/job/PROJECT_NAME/build`` or ``https://JENKINS_URL/job/gitlab-plugin/buildWithParameters``, as this will bypass the plugin completely.

#### Freestyle and Pipeline jobs
1. In the *Build Triggers* section:
    * Select *Build when a change is pushed to GitLab*
    * Copy the *GitLab webhook URL* shown in the UI (see [here](#webhook-url) for guidance)
    * Use the check boxes to trigger builds on *Push Events* and/or *Created Merge Request Events* and/or *Accepted Merge Request Events* and/or *Closed Merge Request Events*
    * Optionally use *Rebuild open Merge Requests* to enable re-building open merge requests after a push to the source branch
    * If you selected *Rebuild open Merge Requests* other than *None*, check *Comments*, and specify the *Comment for triggering a build*.  A new build will be triggered when this phrase appears in a commit comment.  In addition to a literal phrase, you can also specify a Java regular expression
    * You can use *Build on successful pipeline events* to trigger on a successful pipeline run in GitLab. Note that this build trigger will only trigger a build if the commit is not already built and does not set the GitLab status. Otherwise you might end up in a loop
2. Configure any other pre build, build or post build actions as necessary
3. Click *Save* to preserve your changes in Jenkins
4. Create a webhook in the relevant GitLab projects (consult the GitLab documentation for instructions on this), and use the URL you copied from the Jenkins job configuration UI. It should look something like `https://JENKINS_URL/project/yourbuildname`

#### Pipeline Multibranch jobs
Unlike other job types, there is no 'Trigger' setting required for a Multibranch job configuration; just create a webhook in GitLab for push requests which points to [the project's webhook URL.](#webhook-url) When GitLab POSTs to this URL, it will trigger branch indexing for the Jenkins project, and Jenkins will handle starting any builds necessary.

If you want to configure some of the trigger options, such as the secret token or CI skip functionality, you can use a `properties` step. For example:

```groovy
// Define your secret project token here
def project_token = 'abcdefghijklmnopqrstuvwxyz0123456789ABCDEF'

// Reference the GitLab connection name from your Jenkins Global configuration (https://JENKINS_URL/configure, GitLab section)
properties([
    gitLabConnection('your-gitlab-connection-name'),
    pipelineTriggers([
        [
            $class: 'GitLabPushTrigger',
            branchFilterType: 'All',
            triggerOnPush: true,
            triggerOnMergeRequest: false,
            triggerOpenMergeRequestOnPush: "never",
            triggerOnNoteRequest: true,
            noteRegex: "Jenkins please retry a build",
            skipWorkInProgressMergeRequest: true,
            secretToken: project_token,
            ciSkip: false,
            setBuildDescription: true,
            addNoteOnMergeRequest: true,
            addCiMessage: true,
            addVoteOnMergeRequest: true,
            acceptMergeRequestOnSuccess: false,
            branchFilterType: "NameBasedFilter",
            includeBranchesSpec: "release/qat",
            excludeBranchesSpec: "",
        ]
    ])
])
```

#### Multibranch Pipeline jobs with Job DSL

You can use the [Dynamic DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki/Dynamic-DSL) feature of Job DSL to configure the job trigger. See <https://github.com/jenkinsci/gitlab-plugin/blob/master/src/main/java/com/dabsquared/gitlabjenkins/GitLabPushTrigger.java> for the methods you can use.

```groovy
job('seed-job') {

    description('Job that makes sure a service has a build pipeline available')

    parameters {
        // stringParam('gitlabSourceRepoURL', '', 'the git repository url, e.g. git@git.your-domain.com:kubernetes/cronjobs/cleanup-jenkins.git')
    }

    triggers {
        gitlab {
            // This line assumes you set the API_TOKEN as an env var before starting Jenkins - not necessarily required
            secretToken(System.getenv("API_TOKEN"))
            triggerOnNoteRequest(false)
        }
    }

    steps {
        dsl {
            text(new File('/usr/share/jenkins/ref/jobdsl/multibranch-pipeline.groovy').getText('UTF-8'))
        }
    }
}
```

### Build status configuration
You can optionally have your Jenkins jobs send their build status back to GitLab, where it will be displayed in the commit or merge request UI as appropriate.

#### Freestyle jobs
Use 'Publish build status to GitLab' Post-build action to send build status with the given build name back to GitLab.
'Pending' build status is sent when the build is triggered, 'Running' status is sent when the build starts and 'Success' or 'Failed' status is sent after the build is finished.

Also make sure you have chosen the appropriate GitLab instance from the 'GitLab connection' dropdown menu, if you have more than one.

#### Scripted or Declarative Pipeline jobs
**NOTE:** If you use Pipeline global libraries, or if you clone your project's Jenkinsfile from a repo different from the one that contains the relevant source code, you need to be careful about when you send project status. In short, make sure you put your `gitlabCommitStatus` or other similar steps *after* the SCM step that clones your project's source. Otherwise, you may get HTTP 400 errors, or you may find build status being sent to the wrong repo.

#### Scripted Pipeline jobs
* For Pipeline jobs, surround your build steps with the `gitlabCommitStatus` step like this:
    ```groovy
    node() {
        stage('Checkout') { checkout <your-scm-config> }

        gitlabCommitStatus {
           // The result of steps within this block is what will be sent to GitLab
           sh 'mvn install'
        }
    }
    ```
* Or use the `updateGitlabCommitStatus` step to use a custom value for updating the commit status. You could use try/catch blocks or other logic to send fine-grained status of the build to GitLab. Valid statuses are defined by GitLab and documented here: https://docs.gitlab.com/ee/api/commits.html#post-the-build-status-to-a-commit
    ```groovy
    node() {
        stage('Checkout') { checkout <your-scm-config> }

        updateGitlabCommitStatus name: 'build', state: 'pending'
        // Build steps

        updateGitlabCommitStatus name: 'build', state: 'success'
    }
    ```
* Or you can mark several build stages as pending in GitLab, using the `gitlabBuilds` step:
    ```groovy
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
    **Note:** If you put the `gitlabBuilds` block *inside* a node block, it will not trigger until a node is allocated. On a busy system, or one where nodes are allocated on demand, there could be a delay here, and the 'pending' status would not be sent to GitLab right away. If this is a concern, you can move the `gitlabBuilds` block to wrap the node block, and then the status will be sent when Jenkins *starts* trying to allocate a node.

#### Declarative Pipeline jobs
The example below configures the GitLab connection and job triggers. It also sends build status back to GitLab.

**NOTE: You will need to run this job manually once, in order for Jenkins to read and set up the trigger configuration. Otherwise webhooks will fail to trigger the job.**

```groovy
pipeline {
    agent any
    post {
      failure {
        updateGitlabCommitStatus name: 'build', state: 'failed'
      }
      success {
        updateGitlabCommitStatus name: 'build', state: 'success'
      }
      aborted {
        updateGitlabCommitStatus name: 'build', state: 'canceled'
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
          updateGitlabCommitStatus name: 'build', state: 'running'
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

```groovy
    options {
      gitLabConnection('your-gitlab-connection-name')
      gitlabBuilds(builds: ['build', 'test', 'deploy'])
    }
```

If you want to configure any of the optional job triggers that the plugin supports in a Declarative build, use a `triggers` block. The full list of configurable trigger options is as follows:

```groovy
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
      pendingBuildName: "Jenkins",
      cancelPendingBuildsOnUpdate: false,
      secretToken: "abcdefghijklmnopqrstuvwxyz0123456789ABCDEF")
}
```

##### Pending build status for pipelines
To send 'Pending' build status to GitLab when the pipeline is triggered, set a build name to 'Pending build name for pipeline' field in the Advanced-section of the trigger configuration or use pendingBuildName option in the GitLab-trigger configuration in the declarative pipeline.

#### Matrix/Multi-configuration jobs

This plugin can be used with Matrix/Multi-configuration jobs together with the [Flexible Publish](https://plugins.jenkins.io/flexible-publish) plugin which allows you to run publishers after all axis jobs are done. Configure the *Post-build Actions* as follows:

1. Add a *Flexible publish* action
2. In the *Flexible publish* section:
      1. *Add conditional action*
      2. In the *Conditional action* section:
          1. Set *Run?* to *Never*
          2. Select *Condition for Matrix Aggregation*
          3. Set *Run on Parent?* to *Always*
          4. Add GitLab actions as required

### See also
-   [Violation Comments to GitLab
    Plugin](https://wiki.jenkins.io/display/JENKINS/Violation+Comments+to+GitLab+Plugin) for
    pipeline and job DSL examples.

## Advanced features
### Branch filtering
Triggers may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, when you configure the GitLab trigger, you can choose 'Filter branches by name' or 'Filter branches by regex.' Filter by name takes comma-separated lists of branch names to include and/or exclude from triggering a build. Filter by regex takes a Java regular expression to include and/or exclude.

**Note:** This functionality requires access to GitLab and a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to add branch filters. For Pipeline jobs, the configuration must be saved *and* the job must be run once before the list is populated.

### Build when tags are pushed
In order to build when a new tag is pushed:
1. In the GitLab webhook configuration, add 'Tag push events'
2. In the job configuration under 'Source code management':
    1. Select 'Advanced...' and add '`+refs/tags/*:refs/remotes/origin/tags/*`' as the Refspec
    2. You can also use 'Branch Specifier' to specify which tag need to be built (example 'refs/tags/${TAGNAME}')

### Add a note to merge requests

To add a note to GitLab merge requests after the build completes, select 'Add note with build status on GitLab merge requests' from the optional Post-build actions. Optionally, click the 'Advanced' button to customize the content of the note depending on the build result.

#### Pipeline jobs - addGitLabMRComment

```groovy
addGitLabMRComment(comment: 'The pipeline was run on Jenkins')
```

Note that it requires that the build be triggered by the GitLab MR webhook, not the push webhook (or manual build).
Please also note that it currently does *not* work with Multibranch Pipeline jobs, because MR hooks won't trigger.

### Accept merge request
To accept a merge request when build is completed select 'Accept GitLab merge request on success' from the optional Post-build actions.

#### Pipeline jobs
For pipeline jobs two advanced configuration options can be provided
1. **useMRDescription** - Adds the merge request description into the merge commit, in a similar format as would be recieved by selecting 'Modify commit message' followed by 'include description in commit message' in GitLab UI  
2. **removeSourceBranch** - Removes the source branch in GitLab when the merge request is accepted

```groovy
acceptGitLabMR(useMRDescription: true, removeSourceBranch: true)
```

### Notify Specific project by a specific GitLab connection
You can specify a map of project builds to notify a variety of GitLab repositories which could be located on different servers. This is useful if you want to create a complex CI/CD which involves several Jenkins and GitLab projects, see examples bellow:

* Notify several GitLab projects using GitLab connection data from the trigger context:
```groovy
gitlabCommitStatus(name: 'stage1',
        builds: [
            [projectId: 'test/test', revisionHash: 'master'],
            [projectId: 'test/utils', revisionHash: 'master'],
        ])
    {
            echo 'Hello World'
    }
```

* Notify several GitLab projects using specific GitLab connection:
```groovy
gitlabCommitStatus( name: 'stage1', connection:gitLabConnection('site1-connection'),
        builds: [
            [projectId: 'test/test', revisionHash: 'master'],
            [projectId: 'test/utils', revisionHash: 'master'],
        ])
    {
            echo 'Hello World'
    }
```

* Notify several GitLab repositories located on different GitLab servers:
```groovy
gitlabCommitStatus(
        builds: [
            [name:'stage1',connection:gitLabConnection('site1-connection'), projectId: 'group/project1', revisionHash: 'master'],
            [name:'stage1',connection:gitLabConnection('site2-connection'), projectId: 'group/project1', revisionHash: 'master'],
            [name:'stage1',connection:gitLabConnection('site2-connection'), projectId: 'test/test', revisionHash: 'master'],
            [name:'stage1',connection:gitLabConnection('site2-connection'), projectId: 'test/utils', revisionHash: 'master'],
        ])
    {
            echo 'Hello World'
    }
```

### Cancel pending builds on merge request update
To cancel pending builds of the same merge request when new commits are pushed, check 'Cancel pending merge request builds on update' from the Advanced-section in the trigger configuration.
This saves time in projects where builds can stay long time in a build queue and you care only about the status of the newest commit.

## Compatibility

Version 1.2.1 of the plugin introduces a backwards-incompatible change
for Pipeline jobs. They will need to be manually reconfigured when you
upgrade to this version. Freestyle jobs are not impacted.

## Contributing to the Plugin

Detailed instructions for code and documentation contributions to the plugin are available in the [contributing guide](CONTRIBUTING.md).

## Changelog

For recent versions, see [GitHub Releases](https://github.com/jenkinsci/gitlab-plugin/releases).

For versions 1.5.21 and older, see the [historical changelog](https://github.com/jenkinsci/gitlab-plugin/blob/gitlab-plugin-1.5.34/CHANGELOG.md).
