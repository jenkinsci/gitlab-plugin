gitlab-plugin
=====================

This plugin emulates Jenkins as a GitlabCI Web Service to be used with GitlabHQ.

[![Build Status](https://travis-ci.org/DABSquared/gitlab-plugin.svg?branch=master)](https://travis-ci.org/DABSquared/gitlab-plugin) 
[![Gitter chat](https://badges.gitter.im/DABSquared/gitlab-plugin.png)](https://gitter.im/DABSquared/gitlab-plugin)


Current Supported GitLabCI Functions
=====================
* `/project/PROJECT_NAME/builds/COMMIT_SHA1/status.json`
* `/project/PROJECT_NAME/builds/status.png?ref=BRANCH_NAME`
* `/project/PROJECT_NAME/builds/status.png?sha1=COMMIT_SHA1`
* `/project/PROJECT_NAME/builds/COMMIT_SHA1` redirects to build page.


* `/project/PROJECT_NAME`    In order for it to build properly on push you need to add this as a seperate web hook for just merge requests.

Major Help Needed
=====================
I would like this project to be able to handle building merge requests and regular pushes. In order to do this I need a way to configure the git plugin via code to merge two branches together before a build. Much like the RevisionParameterAction.java in the git plugin, we need a class that takes to branches, a source and a target, and can be passed as a build action. I have started an issue for the Git plugin here: https://issues.jenkins-ci.org/browse/JENKINS-23362 If you know of a way to do this please PM on twitter at @bass_rock. All the other necessary code exists in this repo and works.

Configuring access to Gitlab
=======================================

Optionally, the plugin communicates with the Gitlab server in order to fetch additional information. At this moment, this information is limited to fetching the source project of a Merge Request, in order to support merging from forked repositories. 

To enable this functionality, a user should be set up on Gitlab, which adequate permissions to access the repository. On the global configuration screen, supply the gitlab host url ``http://your.gitlab.server`` and the API token of the user of choice.

Using it With A Job
=====================
* Create a new job by going to ``New Job``
* Set the ``Project Name``
* Feel free to specify the ``GitHub Project`` url as the url for the Gitlab project (if you have the GitHub plugin installed)
* In the ``Source Code Management`` section:
    * Click ``Git`` and enter your Repository URL and in Advanced set its Name to ``origin``
    * Add a second Repository with URL ``${gitlabSourceRepoURL}`` and name (in Advanced) ``${gitlabSourceRepoName}`` if you want to be able to merge from forked repositories (this **requires** configuring communication to the Gitlab server)
    * In ``Branch Specifier`` enter ``origin/${gitlabSourceBranch}`` or ``${gitlabSourceRepoName}/${gitlabSourceBranch}``
    * In the ``Additional Behaviours`` section:
        * Click the ``Add`` drop down button and the ``Merge before build`` item
        * Specify the name of the repository as ``origin`` (if origin corresponds to Gitlab) and enter the ``Branch to merge to`` as ``${gitlabTargetBranch}``
* In the ``Build Triggers`` section:
    * Check the ``Build when a change is pushed to GitLab.``
    * Use the check boxes to trigger builds on Push and/or Merge Request events
* In GitLab go to the project ``Settings``
    * Click on ``Services``
    * Click on ``GitLab CI``
        * For ``token`` put any random string (This is not yet functioning)
        * For ``Project URL`` put ``http://JENKINS_URL/project/PROJECT_NAME``
    * Click on ``Web Hooks``
        * Add a ``Web Hook`` for ``Merge Request Events`` to ``http://JENKINS_URL/project/PROJECT_NAME``  (GitLab for some reason does not send a merge request event with the GitLab Service)
* Configure any other pre build, build or post build actions as necessary
* ``Save`` to preserve your changes

Branch filtering
================

Triggers from push events may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, a list of all branches on the remote repository is displayed under ``Build when a change is pushed to GitLab.``. It is possible to select multiple branches by holding Ctrl and clicking. 

This functionality requires accessing the Gitlab server (see [above](#configuring-access-to-gitlab)) and for the time being also a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to select the allowed branches. For existing projects, all branches are allowed to push by default.

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
