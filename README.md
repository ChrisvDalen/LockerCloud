# LockerCloud

This project is a Spring Boot application for managing files.

## HTTPS and WebSocket security

The application exposes HTTPS on port `8443`. Provide a PKCS#12 keystore named
`keystore.p12` in the project root (or set the `SSL_KEYSTORE` environment
variable to its location) before running the application. The relevant
configuration is in `src/main/resources/application.properties`.

When running in Docker, the container also exposes port `8443` for secure
connections. WebSocket connections should use the `wss://` protocol when the
server is accessed via HTTPS.

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
## SSL Socket Server

The application also starts an SSL socket server on port `9000` at runtime. This server exposes simple commands for file upload, download, listing and deletion without using HTTP. Each connection accepts a single command in the form:

```
UPLOAD <filename> <length>\n<bytes...>
DOWNLOAD <filename>\n
DELETE <filename>\n
LIST\n
```

Responses are plain text or raw bytes. The server uses the same `keystore.p12` for TLS encryption.

YourPasswordHere = password;