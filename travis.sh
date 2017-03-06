#!/usr/bin/env bash

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
fi

./mvnw install -Dmaven.javadoc.skip=true -B

