gitlab-jenkins-plugin
=====================

This plugin emulates Jenkins as a GitlabCI Web Service to be used with GitlabHQ.

[![Build Status](https://travis-ci.org/DABSquared/gitlab-jenkins-plugin.svg?branch=master)](https://travis-ci.org/DABSquared/gitlab-jenkins-plugin) 
[![Gitter chat](https://badges.gitter.im/DABSquared/gitlab-jenkins-plugin.png)](https://gitter.im/DABSquared/gitlab-jenkins-plugin)


Current Supported GitLabCI Functions
=====================
* `/project/PROJECT_NAME/builds/COMMIT_SHA1/status.json`
* `/project/PROJECT_NAME/builds/status.png?ref=BRANCH_NAME`
* `/project/PROJECT_NAME/builds/status.png?sha1=COMMIT_SHA1`
* `/project/PROJECT_NAME/builds/COMMIT_SHA1` redirects to build page.


* `/project/PROJECT_NAME/build`    In order for it to build properly on push you need to add this as a seperate web hook. For some reason GitLab is not sending the webhook.

Major Help Needed
=====================
I would like this project to be able to handle building merge requests and regular pushes. In order to do this I need a way to configure the git plugin via code to merge two branches together before a build. Much like the RevisionParameterAction.java in the git plugin, we need a class that takes to branches, a source and a target, and can be passed as a build action. I have started an issue for the Git plugin here: https://issues.jenkins-ci.org/browse/JENKINS-23362 If you know of a way to do this please PM on twitter at @bass_rock. All the other necessary code exists in this repo and works. We just need the merge requests to work and we can put a V1.0 on this thing!

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

1. Fork it ( https://github.com/[my-github-username]/gitlab-jenkins-plugin/fork )
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

Contributors
=====================

* @bass_rock, base ground work.
* @DABSquared, company sponsoring development.
