#!/usr/bin/env bash

./mvnw verify -Prelease=${TRAVIS_TAG}

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw install -Prelease=${TRAVIS_TAG}
fi

