#!/usr/bin/env bash
export MAVEN_OPTS=-Dmaven.javadoc.skip=true -B

./mvnw verify

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
    ./mvnw install
fi

