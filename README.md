gitlab-jenkins-plugin
=====================

This plugin emulates Jenkins as a GitlabCI Web Service to be used with GitlabHQ.

[![Build Status](https://travis-ci.org/DABSquared/gitlab-jenkins-plugin.svg?branch=master)](https://travis-ci.org/DABSquared/gitlab-jenkins-plugin) 


Current Supported GitLabCI Functions
=====================
* `/project/PROJECT_NAME/builds/COMMIT_SHA1/status.json`
* `/project/PROJECT_NAME/builds/status.png?ref=BRANCH_NAME`


Planned GitLabCI Functions
=====================
* `/project/PROJECT_NAME/build`



Help Needed
=====================

* `/projects/` - seems to be already used by Jenkins, A way to use this path would be awesome
* `?token=XYZ` - Can not find a way to include a token parameter on an AbstractProject to security check without an extra plugin configuration
* `/PROJECT_NAME/`  should really be /PROJECT_ID_NUMBER/ - Can not find a project id number on an AbstractProject to use here instead.



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