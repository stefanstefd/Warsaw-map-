#!/bin/bash
# Warsaw Transport Map - Run Script

echo "Starting Warsaw Transport Map Application..."
cd "$(dirname "$0")"
export PATH=$PATH:$(pwd)/apache-maven-3.9.4/bin
mvn javafx:run
y