# LockerCloud

This project is a Spring Boot application for managing files.

## HTTPS and WebSocket security

The application exposes HTTPS on port `8443`. Provide a PKCS#12 keystore named
`keystore.p12` in the project root (or set the `SSL_KEYSTORE` environment
variable to its location) before running the application. The relevant
configuration is in `src/main/resources/application.properties`.

When running in Docker, the container exposes ports `8443` (HTTPS) and `9000`
for the socket server. WebSocket connections should use the `wss://` protocol
when the server is accessed via HTTPS. Set the `KEYSTORE_PASSWORD` environment
variable to the keystore's password.

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
The compose file mounts `keystore.p12` into the container at startup. It exposes
ports `8443` and `9000` and expects the environment variable
`KEYSTORE_PASSWORD` to match the keystore password. Ensure the file is present
in the project root (or adjust `SSL_KEYSTORE` accordingly).

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
to verify that commands were received.

HTTP controllers are **disabled** unless the `http` Spring profile is active.
If you want to use the web interface or REST API, set the environment variable
`SPRING_PROFILES_ACTIVE` to include `http` (for example `prod,http`). The
provided `docker-compose.yml` sets this variable to `prod` only, so by default
the socket server is the only available interface.
