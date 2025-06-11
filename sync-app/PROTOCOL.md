# Custom Sync Protocol

The server and client exchange simple text messages inspired by HTTP.

```
<Start line>\n
Header1: Value1\n
Header2: Value2\n
\n
<optional body>
```

Supported commands:

- `GET /download?file=<path>`
- `POST /upload`
- `POST /sync`
- `POST /listFiles`
- `DELETE /delete?file=<path>`

Headers used:

- `Content-Length`
- `Content-Disposition`
- `Checksum` (MD5)
- `Host`

Large files may be split into 8MB chunks, each with its own checksum.
