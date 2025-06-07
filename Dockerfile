# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-alpine
LABEL authors="cvandalen"
# Create a directory for the application
WORKDIR /app

# Copy the packaged jar file into the container
ARG JAR_FILE=target/LockerCloud-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Expose the secure port your Spring Boot application will run on
EXPOSE 8443

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
