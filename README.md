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

All file operations are performed over a raw SSL socket on port `9000`. The
server implements the commands described in [PROTOCOL.md](PROTOCOL.md). Connect
using `openssl s_client` or any SSL capable socket library and send one of the
following commands:

```
UPLOAD <filename> <length>\n<bytes...>
DOWNLOAD <filename>\n
DELETE <filename>\n
LIST\n
```

The server responds with `OK` or the requested data. Enable application logging
to verify that commands were received. HTTP endpoints are disabled by default;
set `spring.profiles.active=http` if you wish to use the legacy MVC layer.
