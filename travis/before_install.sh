#!/usr/bin/env bash

if [[ -d $DOCKER_CACHE_DIR ]]; then
    ls $DOCKER_CACHE_DIR/*.tar.gz | xargs -I {file} sh -c "zcat {file} | docker load";
fi
