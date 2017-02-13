#!/usr/bin/env bash

./mvnw verify -Drelease=${TRAVIS_TAG}

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw install -Drelease=${TRAVIS_TAG}
fi

