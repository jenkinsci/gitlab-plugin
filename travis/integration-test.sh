#!/usr/bin/env bash

./mvnw integration-test -P integration-test -Dgitlab.version=$GITLAB_VERSION -Dfindbugs.skip=true -Dmaven.javadoc.skip=true
