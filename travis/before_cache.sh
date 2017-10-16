#!/usr/bin/env bash

rm -rf $HOME/.m2/repository/com/dabsquared/gitlabjenkins
mkdir -p $DOCKER_CACHE_DIR
docker images -a --filter='dangling=false' --format '{{.Repository}}:{{.Tag}} {{.ID}}' | xargs -n 2 -t sh -c 'test -e $DOCKER_CACHE_DIR/$1.tar.gz || docker save $0 | gzip -2 > $DOCKER_CACHE_DIR/$1.tar.gz'
