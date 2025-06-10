# LockerCloud

LockerCloud is a small file sharing server implemented with plain Java sockets. Communication uses a lightweight HTTP-like protocol over TCP. A reference client demonstrates file upload and download.

## Building

The project is now a plain Maven module. Run `mvn clean package` to build a runnable JAR:

```bash
mvn clean package
```

The jar `target/LockerCloud-0.0.1-SNAPSHOT.jar` contains all classes and can be executed directly.

## Running the server

Create a `config.properties` file (one is provided) to configure the port, storage path and optional AES key. Start the server with:

```bash
java -jar target/LockerCloud-0.0.1-SNAPSHOT.jar config.properties
```

Uploaded files are stored in the directory specified by `storagePath`.

## Running the client

The simple `SocketClient` class can be used to upload or download files. It expects the host, port and optional AES key from the configuration file. Example:

```bash
java -cp target/LockerCloud-0.0.1-SNAPSHOT.jar org.soprasteria.avans.lockercloud.socketapp.SocketClient
```

Edit the client code to call `upload()` or `download()` as needed.

## Protocol

Requests mimic HTTP messages. Examples:

```text
POST /upload HTTP/1.1
Host: server
Content-Length: <bytes>
Content-Disposition: form-data; name="file"; filename="name"

<file bytes>
```

```text
GET /download?file=name HTTP/1.1
Host: server
```

```text
DELETE /delete?file=name HTTP/1.1
Host: server
```

```text
POST /listFiles HTTP/1.1
Host: server
```

Responses start with `HTTP/1.1 200 OK` and may include headers `Content-Length`, `Content-Disposition` and `Checksum`. The body holds the requested data. If an `aesKey` is configured, file bytes are encrypted using AES.
