# Quick test environment setup using Docker

In order to test the plugin on different versions of `GitLab` and `Jenkins` you may want to use `Docker` containers.

A example docker-compose file is available at `gitlab-plugin/src/docker` which allows to set up instances of the latest `GitLab` and `Jenkins` versions.

If they don't already exist, create the following directories and make sure the user that Docker is running as owns them:
* /srv/docker/gitlab/postgresql
* /srv/docker/gitlab/gitlab
* /srv/docker/gitlab/redis
* /srv/docker/jenkins

To start the containers, run `docker-compose up -d` from the `docker` folder. If you have problems accessing the services in the containers, run `docker-compose up` by itself to see output from the services as they start.

## Access GitLab

To access `GitLab`, point your browser to `http://localhost:10080` and log in with `root` as the username and `password` as the password. Then create a user for Jenkins, impersonate that user, get its API key, set up test repos, etc. When creating webhooks to trigger Jenkins jobs, use `http://jenkins:8080` as the base URL.

If you have trouble cloning a GitLab repository, it may be because you have a leftover host key from an SSH connection to a previous installation of GitLab in Docker. To troubleshoot, run `ssh -vT git@localhost -p 10022`.

## Access Jenkins

To see `Jenkins`, point your browser to `http://localhost:8080`. Jenkins will be able to access GitLab at `http://gitlab`.
Note: you need to change the security settings in `Admin -> Settings -> Network -> Outbound Requests -> Allow requests to the local network from hooks and services` in order for local webhooks to work.

For more information on the supported `Jenkins` tags and how to configure the containers, visit https://hub.docker.com/r/jenkins/jenkins/.

