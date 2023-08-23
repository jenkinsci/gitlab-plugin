# GitLab Plugin Contribution Guide
## Understanding the Plugin

The plugin is using the [GitLab4J-API](https://github.com/gitlab4j/gitlab4j-api) under the hood to interact with GitLab.

Here are some basics you should be aware of while traversing the codebase : 

1. `org.kohusuke.Stapler` is a web framework used by Jenkins to handle HTTP requests and route them to appropriate handlers or actions. The Stapler Dispatcher is responsible for receiving incoming HTTP requests, parsing the request URL, and determining the appropriate action or handler to process the request. Stapler Dispatcher operations refer to the internal processes performed by the Dispatcher to handle HTTP requests. This includes URL parsing, routing, resolving paths to actions or handlers, invoking methods, and generating responses.

2. In the plugin, Jelly script files are used to define the user interface of some parts of the system. These Jelly files describe what configuration options should be presented to the user and how they should be laid out. This allows Jenkins to generate the configuration pages for each plugin dynamically based on these scripts. `StandardListBoxModel` is a class provided by the CloudBees Credentials Plugin, which is used to generate a drop-down list of credentials for a user to choose from. It allows users to select credentials that have already been created in Jenkins, rather than manually entering them every time. `hudson.util.ListBoxModel` is a utility class for generating a list of options in a drop-down list. It provides a set of common methods for adding options to the list.

3. `@ExportedBean` is a Jenkins-specific annotation that marks a class as exportable via a remote API. It is used to make the class's properties and methods accessible to external systems that interact with Jenkins, such as clients that use the Jenkins REST API.

4. `hudson.triggers.SCMTrigger` is a class in the Jenkins core library that provides the functionality for periodically polling a source code management (SCM) system for changes, and triggering builds when changes are detected.

5. In Jenkins, the use of raw HTML is typically not allowed for security reasons, as it can be used to inject malicious code or perform cross-site scripting (XSS) attacks. Instead, Jenkins provides a safe way to display formatted text using the MarkupFormatter API. The `EscapedMarkupFormatter` is one implementation of this API that escapes any potentially harmful characters in the input string, making it safe to display in the Jenkins UI. It replaces special characters like <, >, &, and " with their corresponding HTML entities so that they are not interpreted as HTML tags or attributes.

6. `hudson.util.Secret` is a class in the Jenkins API that provides a way to securely store sensitive information such as passwords, access tokens, or API keys.

7. The `@DataBoundConstructor` annotation is used in Jenkins plugin development to indicate a constructor that should be used for data binding when the plugin is loaded. In Jenkins, data binding is the process of taking user input from the web UI and converting it into an object that can be used by the plugin code. This allows for easier configuration of the plugin and enables users to store plugin configuration data in Jenkins.

8. `hudson.security.ACL` stands for Access Control List, and it is a model for defining who has access to what resources. `Jenkins.ACL` is used to define security policies and access controls in Jenkins. It defines a set of rules that dictate who has access to which resources, based on their identity, group membership, or other attributes. ACL defines the concept of "authenticated" users, who are users that have successfully authenticated themselves to Jenkins using a valid username and password or other authentication method.

9. `hudson.model.Item` and `hudson.model.ItemGroup` are used to represent different types of resources in Jenkins, such as jobs, pipelines, folders, and organizations. These resources can have different levels of access control applied to them, and ACL is used to define these access controls.

10. Backward compatibility is required in the plugin to ensure that the configuration data saved by previous versions of the plugin can still be loaded and used by the current version. The `readResolve()` method is called when the configuration is loaded, and it sets a default value for the `useAuthenticatedEndpoint` field if it is null. This ensures that configuration data saved by previous versions of the plugin that did not include this field will still work correctly with the current version. Without this backward compatibility, users who upgrade to the latest version of the plugin may experience configuration issues or data loss.

11. In Jenkins, `load()` is a method used to load configuration data for a plugin from disk. By calling `load()` in the constructor, the plugin ensures that any saved configuration is loaded into the new instance of the class. 

Some basic Maven commands you should know :

`mvn compile` - This command is responsible for compiling the source code.
`mvn clean install` - This command cleans the project by removing any previously generated files and then installs the project's artifacts (e.g., JAR, WAR) into the local Maven repository. It also resolves dependencies and compiles the source code. This is typically used to build and package the project for local development or testing.
`mvn clean install -DskipTests` - Similar to the previous command, but it skips running the unit tests during the build process.
`mvn clean install -DskipITs` - Similar to the previous command, but it skips running the integration tests (ITs) during the build process.

`mvn clean test` - This command cleans the project, compiles the source code, and then runs the unit tests. It's useful when you want to execute the unit tests without installing the project artifacts in the local repository.
`mvn clean test -DskipITs` - Similar to the previous command, but it skips running the integration tests while executing the unit tests.
`mvn clean test -Dtest=<name of the test>` - This command is used to run a specific unit test by specifying its name. It's handy when you want to focus on a specific test case during development or debugging.

`mvn clean verify`-  This command cleans the project, compiles the source code, runs the unit tests and integration tests, and performs additional verification steps spotless. It's often used before deploying the application to a testing or staging environment. This creates a `.hpi` file for deployment.

For learning more about Maven [refer](https://maven.apache.org/guides/index.html).

`mvn hpi:hpi` - This command is specific to Jenkins plugins development. It packages the Jenkins plugin (hpi) file, which can then be deployed to a Jenkins instance for testing.

> Note : If Jenkins or GitLab are running locally you can use [ngrok](https://ngrok.com) to expose the url to the internet. Another benefit of using ngrok is that you can visit `localhost:4040` to see the payload and other webhook related details. This can be specifically useful when writing tests for the plugin incase the JSON sent by GitLab has changed.

> Manual Uninstall : Incase the plugin has some major problem which is not letting you to uninstall the plugin you can manually uninstall the plugin by going inside you Jenkins filesystem (Files section in docker desktop if using docker instance of Jenkins) and navigating to `/var/jenkins_home/plugins/gitlab-plugin/` where you will find `gitlab-plugin.jpi` file. Just delete it and restart your Jenkins controller. 

## Testing With Docker

See [this](https://github.com/jenkinsci/gitlab-plugin/tree/master/src/docker/README.md)

## Debugging

When testing with a Docker Jenkins instance, the Intellij's debugger can be setup in the following way to debug the codebase:
* From the main menu, select Run -> Edit Configurations.
* In the Run/Debug Configurations dialog, click the Add New Configuration button `+` and select Remote JVM Debug.
* Enter any relevant name, the Host (the address of the machine where the host app will run. If running it on the same machine, it needs to be localhost. If the program is running on another machine, specify its address here) and the Port (by default use 50000). The Command Line argument will be automatically setup.
* Enter Apply!

The functional tests in the GitLab Plugin's codebase can also be debugged in the following way :
* From the main menu, select Run -> Edit Configurations.
* In the Run/Debug Configurations dialog, click the Add New Configuration button `+` and select JUnit.
* Change the working directory to gitlab plugin's folder.
* Enter the required test class to be debugged.
* Enter Apply!

Now start your Jenkins instance and debugger and you should get something like this - `Connected to the target VM, address: 'localhost:50000', transport: 'socket'`.

Breakpoints can now be set up to halt the debugger at the required break point to understand the flow of the program.
## Logging

To enable debug logging in the plugin:

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'GitLab plugin' or whatever you want for the name
4. On the next page, enter 'com.dabsquared.gitlabjenkins' for Logger, set log level to FINEST, and save
5. Then click on your GitLab plugin log, click 'Clear this log' if necessary, and then use GitLab to trigger some actions
6. Refresh the log page and you should see output.

> Note : You can also view your detailed Jenkins logs when using Jenkins Docker instance by simply going to Logs section of your Jenkins container in your Docker Desktop.

To enable Stapler Dispatcher operations you can goto Manage Jenkins -> Script Console and use this script - `System.setProperty('org.kohsuke.stapler.Dispatcher.TRACE', 'true') and click on Run to execute the script.

Once you have set the `org.kohsuke.stapler.Dispatcher.TRACE`` property to `true`, you can view the detailed logging in the Jenkins log files. Here's how you can access the log files:

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'Stapler Dispatcher' or whatever you want for the name
4. On the next page, enter 'org.kohsuke.stapler.Dispatcher' for Logger, set log level to FINEST/ALL, and save
5. Then click on your Stapler Dispatcher log, and you should see the logs. If not then please refresh the page.
## Interactive Testing 

For testing the development version of the plugin you can manually install the plugin in your Jenkins controller using its .hpi file.

> Note : The `.hpi` and '.jpi' file extensions are different. You deploy the `.hpi` file in your Jenkins controller and its stored as `.jpi` file in the Jenkins filesystem. 

The .hpi can be generated using `mvn hpi:hpi` which will be stored in `/target/`. To install it manually in your Jenkins instance follow : 
* Goto Manage Jenkins -> Plugins -> Advanced settings.
* In the Deploy Plugin section choose the generated gitlab-plugin.hpi file and deploy.

## Testing the Proxy Server Interactively

To hide the local Jenkins instance behind a proxy server, we can use [Ngnix Proxy Manager](https://nginxproxymanager.com/guide/#quick-setup) to setup a docker instance of Ngnix Proxy Manager which will manage the Ngnix Proxy server in the same container.

Here is how you can create a proxy host in the Proxy Manager :
1. Once you have signed in with default credentials in your Ngnix Proxy Manager docker instance, goto Hosts -> Add Proxy Host.
2. Enter a public Domain name of Ngnix Proxy Server (this should be the domain name that GitLab will use to send requests to your Jenkins server. If you're using ngrok - `ngrok http 80`), its IP Address (this should be the IP address of your Jenkins server from the perspective of the Ngnix Proxy Manager. If they are running in different Docker containers on the same Docker network, you can use the Jenkins container's name as the hostname otherwise simply use `host.docker.internal`) and the Port its listening from (This should be the port that your Jenkins server is listening on, by default - 8080/50000).
3. Enter Save!

By default this setup would provide you with Unauthenticated Proxy Server, to enable Authorization follow these steps :
1. Goto `Access Lists` tab in Ngnix Proxy Manager.
2. Add the Access List with a suitlabe Name and Authorization credentials.
3. Enter Save!
4. Goto the Proxy Host you setup earlier and edit it with new Access List which should be available in the drop down.
5. Enter Save!

Now in your GitLab's WebHook Settings use the url of
the proxy server (the url provided by ngrok) instead of Jenkins url.

With this setup, GitLab will send webhook requests to your Nginx proxy server, which will then forward those requests to Jenkins based on the proxy host configuration you set up in Nginx Proxy Manager. This way, Nginx acts as an intermediary (reverse proxy) between GitLab and Jenkins, hiding the actual IP address and details of your Jenkins server from external access.

## Testing in Production

For testing out the changes in actual production environment you have to setup actual Jenkins instance and actual GitLab Server. 

## Contributing to the Plugin

Plugin source code is hosted on [GitHub](https://github.com/jenkinsci/gitlab-plugin).
New feature proposals and bug fix proposals should be submitted as
[GitHub pull requests](https://help.github.com/articles/creating-a-pull-request).
Fork the repository on GitHub, prepare your change on your forked
copy, and submit a pull request (see [here](https://github.com/jenkinsci/gitlab-plugin/pulls) for open pull requests). Your pull request will be evaluated by the [plugin's CI job](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fgitlab-plugin/).

If you are adding new features please make sure that they support Jenkins Pipeline jobs.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md) for some information.


Before submitting your change make sure that:
* your changes work with the oldest and latest supported GitLab version
* new features are provided with tests
* refactored code is provided with regression tests
* the code formatting follows the plugin standard
* imports are organised
* you updated the help docs
* you updated the README
* you have used spotbugs to see if you haven't introduced any new warnings. if you have then you can use `mvn spotbugs:gui` to see the errors and warnings clearly.
* you can run `mvn spotless:apply` to confirm that the code formatting is as expected

## Release Workflow

To perform a full plugin release, maintainers can run ``mvn release:prepare release:perform`` To release a snapshot, e.g. with a bug fix for users to test, just run ``mvn deploy``

For information related to manual release refer [this](https://www.jenkins.io/doc/developer/publishing/releasing-manually).
