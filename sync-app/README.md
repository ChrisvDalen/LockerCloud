# Sync App

This project contains a small demo implementation of a custom file synchronisation protocol using plain Java sockets and a minimal Angular front‑end.

## Build

```bash
mvn -f sync-app/pom.xml clean package
```

## Run server

```bash
java -jar sync-app/sync-server/target/sync-server-1.0-SNAPSHOT.jar
```

## Run client

```bash
java -jar sync-app/sync-client/target/sync-client-1.0-SNAPSHOT.jar
```

## Angular front‑end

From the `sync-ui` folder run:

```bash
npm install
npm start
```

The UI is available on http://localhost:4200 .

## Docker Compose

After building the server jar (`mvn -f sync-app/pom.xml package`), you can run
both the server and the UI using Docker Compose:

```bash
docker compose -f sync-app/docker-compose.yml up
```

Uploaded files are stored in a named Docker volume so that they persist between
container restarts.
