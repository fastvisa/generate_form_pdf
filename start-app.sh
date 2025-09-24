#!/bin/bash
cd /var/www/generate_form_pdf/generate_form_pdf
export AWS_ACCESS_KEY_ID="$aws_access_key"
export AWS_SECRET_ACCESS_KEY="$aws_secret_key"
export AWS_REGION="$aws_region"
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080" -Dspring.profiles.active=prod