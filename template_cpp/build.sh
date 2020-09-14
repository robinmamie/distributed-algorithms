#!/bin/bash

set -e

# Change the current working directory to the location of the present file
cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

rm -rf target
mkdir target
cd target
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build .
mv src/da_proc ../bin
