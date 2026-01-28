#!/bin/sh

# Set version from latest git tag before running
./scripts/set-version-from-tag.sh

# Run the application
mvn spring-boot:run