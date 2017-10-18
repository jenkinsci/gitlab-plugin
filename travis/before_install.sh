#!/usr/bin/env bash

if [[ -d $DOCKER_CACHE_DIR ]]; then
    ls $DOCKER_CACHE_DIR/*.tar.gz | xargs -I {file} sh -c "zcat {file} | docker load";
fi

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
fi

