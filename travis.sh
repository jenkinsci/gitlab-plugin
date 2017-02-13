#!/usr/bin/env bash

./mvnw verify -Dmaven.javadoc.skip=true -B

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
    ./mvnw install -Dmaven.javadoc.skip=true -B
fi

