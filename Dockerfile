# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml ./
COPY src ./src
RUN mvn -q package

# Runtime stage
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=build /src/target/LockerCloud-0.0.1-SNAPSHOT.jar app.jar
COPY config.properties config.properties
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar", "config.properties"]
