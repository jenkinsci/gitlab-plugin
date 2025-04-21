## Contributing to the Plugin

Plugin source code is hosted on [GitHub](https://github.com/jenkinsci/gitlab-plugin).
New feature proposals and bug fix proposals should be submitted as
[GitHub pull requests](https://help.github.com/articles/creating-a-pull-request).
Fork the repository on GitHub, prepare your change on your forked
copy, and submit a pull request (see [here](https://github.com/jenkinsci/gitlab-plugin/pulls) for open pull requests). Your pull request will be evaluated by the [plugin's CI job](https://ci.jenkins.io/job/Plugins/job/gitlab-plugin/).

If you are adding new features please make sure that they support Jenkins Pipeline jobs.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md) for some information.


Before submitting your change make sure that:
* your changes work with the oldest and latest supported GitLab version
* new features are provided with tests
* refactored code is provided with regression tests
* the code formatting follows the plugin standard (use `mvn spotless:apply` to format the code)
* imports are organised
* you updated the help docs
* you updated the README
* you have used spotbugs to see if you haven't introduced any new warnings

## Testing With Docker

See https://github.com/jenkinsci/gitlab-plugin/tree/master/src/docker/README.md

## Using IntelliJ's Debugger

When testing with a Docker Jenkins instance, the debugger can be setup in the following way:
* From the main menu, select Run -> Edit Configurations.
* In the Run/Debug Configurations dialog, click the Add New Configuration button `+` and select Remote JVM Debug.
* Enter any relevant name, the Host (the address of the machine where the host app will run. If running it on the same machine, it needs to be localhost. If the program is running on another machine, specify its address here) and the Port (by default use 50000). The Command Line argument will be automatically setup.
* Enter Apply.

Now start your Jenkins instance and debugger and you should get something like this - `Connected to the target VM, address: 'localhost:50000', transport: 'socket'`.
Breakpoints can now be set up to halt the debugger at the required break point to understand the flow of the program.  

## Release Workflow

To perform a full plugin release, maintainers can run ``mvn release:prepare release:perform`` To release a snapshot, e.g. with a bug fix for users to test, just run ``mvn deploy``
