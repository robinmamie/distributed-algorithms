#!/bin/bash

# Change the current working directory to the location of the present file
cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd ..

docker build -f docker/Dockerfile -t da_image .
