# LockerCloud

LockerCloud is a small file sharing server implemented with plain Java sockets. It no longer relies on Spring MVC or HTTP. Communication happens over TCP using simple text commands. A reference client is included for testing.

## Building

The project is now a plain Maven module. Run `mvn clean package` to build a runnable JAR:

```bash
mvn clean package
```

The jar `target/LockerCloud-0.0.1-SNAPSHOT.jar` contains all classes and can be executed directly.

## Running the server

Create a `config.properties` file (one is provided) to configure the port, storage path and optional authentication token and AES key. Start the server with:

```bash
java -jar target/LockerCloud-0.0.1-SNAPSHOT.jar config.properties
```

Uploaded files are stored in the directory specified by `storagePath`.

## Running the client

The simple `SocketClient` class can be used to upload or download files. It expects the host, port, token and AES key from the configuration file. Example:

```bash
java -cp target/LockerCloud-0.0.1-SNAPSHOT.jar org.soprasteria.avans.lockercloud.socketapp.SocketClient
```

Edit the client code to call `upload()` or `download()` as needed.

## Protocol

The server understands the following commands over a TCP connection:

```
AUTH <token>
UPLOAD <filename> <length>\n<bytes...>
DOWNLOAD <filename>
DELETE <filename>
LIST
```

Responses are plain text (for commands) or raw bytes (for downloads). If an AES key is configured, all file data is encrypted using AES.
