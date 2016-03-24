#!/usr/bin/env bash

set -e

# ===============================================================
# Launch Gitlab
# ===============================================================

# Launch a postgresql container
docker run --name gitlab-postgresql -d \
    --env 'DB_NAME=gitlabhq_production' \
    --env 'DB_USER=gitlab' --env 'DB_PASS=password' \
    --volume /tmp/docker/gitlab/postgresql:/var/lib/postgresql \
    sameersbn/postgresql:9.4-15

# Launch a redis container
docker run --name gitlab-redis -d \
    --volume /tmp/docker/gitlab/redis:/var/lib/redis \
    sameersbn/redis:latest

# Launch the gitlab container
docker run --name gitlab -d \
    --link gitlab-postgresql:postgresql --link gitlab-redis:redisio \
    --publish 10022:22 --publish 10080:80 \
    --env 'GITLAB_PORT=10080' --env 'GITLAB_SSH_PORT=10022' \
    --env 'GITLAB_SECRETS_DB_KEY_BASE=long-and-random-alpha-numeric-string' \
    --volume /tmp/docker/gitlab/gitlab:/home/git/data \
    sameersbn/gitlab:8.5.8

echo "Point your browser to http://localhost:10080 and login using the default username and password:"
echo "username: root"
echo "password: 5iveL!fe"
