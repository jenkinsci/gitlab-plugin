#!/usr/bin/env bash
./mvnw verify -B -Pall-tests,skip-javadoc-with-tests -Dmaven.javadoc.skip=true
