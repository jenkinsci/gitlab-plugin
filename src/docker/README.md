In order to test the plugin on different versions of `GitLab` and `Jenkins` you may want to use `Docker` containers.

# Quick test environment setup using Docker for Linux/amd64

An example docker-compose file is available at `gitlab-plugin/src/docker` which the user can use to set up instances of the latest `GitLab` version and latest `Jenkins` LTS version for linux/amd64.

If they don't already exist, create the following directories and make sure the user that Docker is running as owns them:
* /srv/docker/gitlab/postgresql
* /srv/docker/gitlab/gitlab
* /srv/docker/gitlab/redis
* /srv/docker/jenkins
To start the containers for Linux, run `docker-compose up -d` from the `docker` folder. If you have problems accessing the services in the containers, run `docker-compose up` by itself to see output from the services as they start, and the latter command is the verbose version of the former.

## Quick test environment setup using Docker for MacOS/arm64

You need to modify the example docker-compose file available at `gitlab-plugin/src/docker` to set up instances of the latest `GitLab` and `Jenkins` versions for MacOS/arm64. 

Due to Apple's System Integrity Protection (SIP), the suggested paths cannot be simply created and accessed, so you may need to use the home directory (~) as a root for the new directories to be created.

In the `docker-compose.yml` file :
    1. Change the ports to 
      - '55580:80'
      - '55522:22'
      - '55443:443'
      as the browser may block the ports in original docker-compose file.
    2. Change the gitlab volumes to 
        `/Users/yourusername/srv/docker/gitlab/config:/etc/gitlab`
        `/Users/yourusername/srv/docker/gitlab/logs:/var/log/gitlab`
        `/Users/yourusername/srv/docker/gitlab/data:/var/opt/gitlab`
    3. Change the jenkins volumes to 
        `/Users/yourusername/srv/docker/jenkins:/var/jenkins_home`
    4. In your Docker-Desktop go to `Settings > General > Choose file sharing implementation for your containers` and switch to osxfs (Legacy). As `osxfs (Legacy)` utilizes more resources of the system, make sure the assigned resources are sufficient by going to `Settings > Resources` and make suitable adjustments where necessary, otherwise Docker Desktop may go on start mode forever on restarting.
    5. Add `shm_size: '5gb'`under gitlab services.

Like the instructions for Linux, for macOS users to start the containers, run `docker-compose up -d` from the `docker` folder. If you have any problems accessing the services in the containers, run `docker-compose up` by itself to see output from the services as they start.

## Access GitLab

To access `GitLab`, you first need to create a user - `root` with some password. To do so, follow the following steps :
    1. In the Gitlab containers terminal inside Docker Desktop, type `gitlab-rails console` and wait for at least a few minutes for the console to start. 
    2. Once the console is started successfully, run the following commands in sequence at the console, noting that there are certain security rules to the password choice:
        a. `user = User.new(username: 'root', email: 'root@root.com', name: 'root', password: 'setyourown', password_confirmation: 'setyourown')`
        b. `user.skip_confirmation!` 
        c. `user.save!`
    3. Now, point your browser to `http://localhost:55580` and log in with `root` as the username and `setyourown` as the password. Then create a user for Jenkins, impersonate that user, get its API key, set up test repos, etc. When creating webhooks to trigger Jenkins jobs, use `http://jenkins:8080` as the base URL.

If you have trouble cloning a GitLab repository, it may be because you have a leftover host key from an SSH connection to a previous installation of GitLab in Docker. To troubleshoot, run `ssh -vT git@localhost -p 55522`.

Please note that it is no longer recommended to use ports 10080 and 10022 even for local testing, as more modern browsers have policies set to block the use of such ports. 

## Access Jenkins

To see `Jenkins`, point your browser to `http://localhost:8080`. Jenkins will be able to access GitLab at `http://gitlab`.

Note: you need to change the security settings in `Admin -> Settings -> Network -> Outbound Requests -> Allow requests to the local network from hooks and services` in order for local webhooks to work.

For more information on the supported `Jenkins` tags and how to configure the containers, visit https://hub.docker.com/r/jenkins/jenkins/.

