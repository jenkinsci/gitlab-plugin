#!/usr/bin/env bash

if [ "$TRAVIS_TAG" != "" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
fi

 ./mvnw verify -Dmaven.javadoc.skip=true -B -P$TEST_PROFILE,skip-javadoc-with-tests

