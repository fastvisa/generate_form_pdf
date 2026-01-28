#!/bin/bash
# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
# Run the JAR file directly instead of using mvn spring-boot:run for better performance
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080" -Dspring.profiles.active=prod 