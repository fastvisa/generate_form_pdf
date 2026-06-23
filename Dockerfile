# Build stage
FROM maven:3.8.5-jdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_LEVEL=INFO
CMD ["java", "-jar", "app.jar", "--server.port=8080", "--spring.profiles.active=prod"]
