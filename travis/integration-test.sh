#!/usr/bin/env bash
if [[ -d $DOCKER_CACHE_DIR ]]; then
    echo "Loading cached images into docker..."
    ls $DOCKER_CACHE_DIR/*.tar.gz | xargs -I {file} sh -c "zcat {file} | docker load";
fi

./mvnw integration-test -P integration-test -Dgitlab.version=$GITLAB_VERSION -Dfindbugs.skip=true -Dmaven.javadoc.skip=true
