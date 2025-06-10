# LockerCloud Protocol

LockerCloud uses a minimal HTTP-style protocol over TCP sockets.
All requests start with a request line followed by optional headers and a blank line.

## Requests
- **POST /upload** - Upload a file. Headers must include `Content-Length` and
  `Content-Disposition` with the file name. The request body contains the bytes.
- **GET /download?file=name** - Download a file.
- **POST /listFiles** - Retrieve a newline separated list of files.
- **DELETE /delete?file=name** - Delete a file from the server.

## Responses
Responses start with a status line such as `HTTP/1.1 200 OK` followed by headers
and an optional body. For downloads the body contains the file bytes and headers
include `Content-Length`, `Content-Disposition` and `Checksum`.
When an AES key is configured, file data in upload and download requests is
encrypted.
