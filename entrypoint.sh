#!/bin/sh

# Load .env into environment for AWS credentials and other config
if [ -f .env ]; then
    set -a
    . .env
    set +a
fi

# Set version from latest git tag before running
./scripts/set-version-from-tag.sh

# Run the application
mvn spring-boot:run