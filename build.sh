#!/bin/bash

## Standard build command for simplicity, can be adapted depending on requirements
docker build --build-arg JAR_FILE=build/libs/*.jar -t dbcfs . && docker run -P -v $(pwd)/data:/mnt -p 8080:8080 -p 34197:34197/udp dbcfs/poc 
