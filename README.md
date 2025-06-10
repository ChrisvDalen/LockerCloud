# LockerCloud

This project is a Spring Boot application for managing files.

## HTTPS and WebSocket security

The application exposes HTTPS on port `8443`. Provide a PKCS#12 keystore named
`keystore.p12` in the project root (or set the `SSL_KEYSTORE` environment
variable to its location) before running the application. The relevant
configuration is in `src/main/resources/application.properties`.

When running in Docker, the container also exposes port `8443` for the web
interface and port `8444` for the secure WebSocket endpoint. WebSocket
connections should use the `wss://` protocol.

### Generating a certificate

The repository does not include a keystore. Generate one with `keytool` and
place the resulting `keystore.p12` in the project root (or point
`SSL_KEYSTORE` to it) **before** starting the application:

```bash
keytool -genkeypair -alias lockercloud -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass <password> -dname "CN=LockerCloud"
```
After creating the keystore, build the project and start the container
(rebuild ensures the latest jar is used):

```bash
mvn clean package
podman compose up --build
```
The compose file mounts `keystore.p12` into the container at startup. Ensure the
file is present in the project root (or adjust `SSL_KEYSTORE` accordingly).
## WebSocket Server

The application exposes the WebSocket endpoint on port `8444`. Clients communicate using JSON messages over `wss://`:

```
{"command":"list"}
{"command":"upload","file":"name","data":"<base64>"}
{"command":"download","file":"name"}
{"command":"delete","file":"name"}
```

Responses are JSON as well (for downloads the file data is Base64 encoded). The server uses the same `keystore.p12` for TLS encryption.


## Web Interface & CLI Usage

Open <https://localhost:8443/> in a browser to use the WebSocket based interface. It connects to the WebSocket endpoint on port `8444`.
You can also use the WebSocket command line client for file operations:

```bash
java -cp target/LockerCloud-0.0.1-SNAPSHOT.jar \
  org.soprasteria.avans.lockercloud.client.WebSocketClientApp list
java -cp target/LockerCloud-0.0.1-SNAPSHOT.jar \
  org.soprasteria.avans.lockercloud.client.WebSocketClientApp upload /path/to/file
java -cp target/LockerCloud-0.0.1-SNAPSHOT.jar \
  org.soprasteria.avans.lockercloud.client.WebSocketClientApp download file.txt /tmp/file.txt
```

The client communicates with the `WebSocketFileServer` using the protocol described in `PROTOCOL.md`.
