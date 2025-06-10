# LockerCloud Socket Protocol

LockerCloud communicates over plain TCP sockets using simple text commands. Every command is terminated by a newline. Some commands are followed by binary data as described below.

## Commands

`AUTH <token>`
: Authenticate using a token configured on the server. The server responds with `OK` or `ERR`.

`UPLOAD <name> <size>`
: Start uploading a file. After the newline the client must send exactly `<size>` bytes of file data. The server replies `OK` when the upload succeeds.

`DOWNLOAD <name>`
: Request a file. The server replies with `<size>\n` followed by the raw bytes.

`LIST`
: Retrieve a list of files. The server sends one file name per line and terminates the list with `END`.

`DELETE <name>`
: Delete a file from the server. The response is `OK` when successful.

If AES encryption is enabled in `config.properties`, file data sent with `UPLOAD` and `DOWNLOAD` is encrypted using the provided key.
