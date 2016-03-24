#!/usr/bin/env bash

set -e

# ===============================================================
# Launch Jenkins
# ===============================================================

docker pull jenkins:1.642.2
docker run --name jenkins-1.642.2 -d -p 8080:8080 -p 50000:50000 -v /src/docker/jenkins:/var/jenkins jenkins:1.642.2

echo "Point your browser to http://localhost:8080 to access Jenkins webinterface"
