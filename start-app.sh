#!/bin/bash
cd /var/www/generate_form_pdf/generate_form_pdf
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080" -Dspring.profiles.active=prod