# LockerCloud Synchronization Protocol

This document describes the protocol used for communication between the client and server components of the LockerCloud application. It is inspired by HTTP and uses a simple message format consisting of a start line, headers and an optional body.

## Commands

| Method | Path         | Purpose                       |
|-------|--------------|-------------------------------|
| GET    | `/download`  | Download a file. Requires query parameter `fileName`. |
| POST   | `/upload`    | Upload a file. Large files can be sent in chunks using the headers described below. |
| POST   | `/sync`      | Request file synchronization. |
| POST   | `/listFiles` | Retrieve the list of files on the server. |
| DELETE | `/delete`    | Delete a file. Requires query parameter `fileName`. |

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

