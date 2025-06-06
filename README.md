# LockerCloud

![Version](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Coverage](https://img.shields.io/badge/coverage-N%2FA-lightgrey)

LockerCloud is a Spring Boot application that exposes REST endpoints and a minimal web UI for managing cloud storage. It provides chunked uploads, checksum verification and real-time notifications over WebSockets.

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Configuration](#configuration)
- [Authors](#authors)

## Features
- Upload and download files or archives
- List, delete and synchronise stored files
- Automatic chunking and checksum verification
- WebSocket notifications for real time updates
- Circuit breaker and retry logic using Resilience4j
- Swagger UI available at `/swagger-ui`

## Requirements
- **Java&nbsp;17+** – LockerCloud is built with Java 17
- **Maven&nbsp;3.9+** – use the supplied `mvnw` wrapper to build
- **Docker** (optional) – for running the provided container setup

## Getting Started
Clone the repository and start the application with:
```bash
./mvnw spring-boot:run
```
The server runs on [http://localhost:8080](http://localhost:8080). The web interface is available on `/` and the API documentation on `/swagger-ui`.

### Docker
Build and run the container with:
```bash
docker-compose up --build
```
Uploads are stored under `./filestorage` on the host machine.

## Running Tests
Execute all tests with:
```bash
./mvnw test
```
Test coverage is not available in this environment because Maven cannot download dependencies.

## Configuration
Application settings reside in [`application.properties`](src/main/resources/application.properties) and include multipart limits and circuit breaker options.

## Authors
Chris van Dalen & Pieter van Oorschot
