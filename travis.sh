#!/usr/bin/env bash

./mvnw verify

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
    ./mvnw install
fi

