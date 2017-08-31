# Quick test environment setup using Docker

In order to test the plugin on different versions of `GitLab` and `Jenkins` you may want to use `Docker` containers.

A example docker-compose file is available at `gitlab-plugin/src/docker` which allows to set up instances of the latest `GitLab` and `Jenkins` versions.

If they don't already exist, create the following directories and make sure the user that Docker is runnning as owns them:
* /srv/docker/gitlab/postgresql
* /srv/docker/gitlab/gitlab
* /srv/docker/gitlab/redis
* /srv/docker/jenkins

To start the containers, run `docker-compose up -d` from the `docker` folder. If you have problems accessing the services in the containers, run `docker-compose up` by itself to see output from the services as they start.

## Access GitLab

To access `GitLab`, point your browser to `http://localhost:10080` and set a password for the `root` user account. Then create a user for Jenkins, impersonate that user, get its API key, set up test repos, etc. 

For more information on the supported `GitLab` versions and how to configure the containers, visit Sameer Naik's github page at https://github.com/sameersbn/docker-gitlab.

## Access Jenkins

To see `Jenkins`, point your browser to `http://localhost:8080`. Jenkins will be able to access GitLab at `http://gitlab`

For more information on the supported `Jenkins` tags and how to configure the containers, visit https://hub.docker.com/r/jenkins/jenkins/.

