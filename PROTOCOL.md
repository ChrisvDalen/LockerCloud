# LockerCloud Socket Protocol

LockerCloud communicates over an SSL socket on port `9000`. Each command is sent as an ASCII line followed by optional binary data. The server replies with `OK`, `ERR` or the requested bytes.

## Commands

- `UPLOAD <filename> <length>` – Send `<length>` raw bytes immediately after the newline. The file is stored on the server and validated with an optional checksum.
- `DOWNLOAD <filename>` – The server responds with the byte length and then the raw bytes of the file.
- `DELETE <filename>` – Removes the specified file and returns `OK`.
- `LIST` – Returns a list of filenames terminated by the line `END`.

## Large file handling

Files larger than 4 GB are uploaded in 10 MB chunks internally. The client does not need to send chunk headers; the server handles splitting and assembling.

## Network failure recovery

Operations are retried up to three times. If a checksum is provided and does not match after upload, the server deletes the file and returns `ERR`.

