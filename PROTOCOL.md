# LockerCloud Synchronization Protocol

This document describes the protocol used for communication between the client and server components of the LockerCloud application. Earlier versions used a custom HTTP-like protocol over raw sockets. The application now communicates via JSON messages over a secure WebSocket connection. Each message contains a `command` field and optional parameters.

## Commands

Each JSON request contains a `command` field. Supported values are:

| Command   | Extra velden             | Doel                                         |
|-----------|-------------------------|----------------------------------------------|
| `upload`  | `file`, `data` (Base64) | Upload een bestand. Grote bestanden kunnen in delen worden verstuurd. |
| `download`| `file`                  | Download een bestand. Respons bevat `data` in Base64. |
| `list`    | *geen*                  | Lijst alle beschikbare bestanden op de server. |
| `delete`  | `file`                  | Verwijder een bestand. |
| `sync`    | metadata JSON           | (optioneel) Synchronisatie van bestanden. |

## Headers

* `Content-Length` – length of the request body in bytes.
* `Content-Disposition` – for uploads, indicates the original file name.
* `Checksum` – MD5 checksum used to validate an uploaded file or download response.
* `Chunk-Index` – (optional) index of the uploaded chunk starting at 1.
* `Chunk-Total` – (optional) total number of chunks for the file.
* `File-Checksum` – (optional) final checksum of the whole file, sent with the last chunk.
* `Host` – hostname of the server.

## Large File Handling

Files larger than **4 GB** are uploaded in chunks. Each chunk is sent in a separate `POST /upload` request. The server stores chunks as `<filename>.partN` and assembles them once the last chunk is received. The final file is validated using `File-Checksum`.

## Network Failure Recovery

The server retries upload and download operations up to three times in case of I/O errors. If a transactional upload with a checksum is requested, the file is only committed when the checksum matches.

