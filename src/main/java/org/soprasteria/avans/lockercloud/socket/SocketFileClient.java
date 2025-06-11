package org.soprasteria.avans.lockercloud.socket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Very small client helper for tests/demos. It speaks the same simple protocol
 * as {@link SocketFileServer}.
 */
public class SocketFileClient implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private OutputStream out;

    public SocketFileClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = socket.getOutputStream();
    }

    public String upload(String fileName, byte[] data) throws IOException {
        String checksum = md5Hex(data);
        for (int attempt = 0; attempt < 3; attempt++) {
            StringBuilder req = new StringBuilder();
            req.append("POST /upload HTTP/1.1\n");
            req.append("Content-Length: ").append(data.length).append('\n');
            req.append("Content-Disposition: form-data; filename=\"").append(fileName).append("\"\n");
            req.append("Checksum: ").append(checksum).append('\n');
            req.append('\n');
            out.write(req.toString().getBytes(StandardCharsets.UTF_8));
            out.write(data);
            out.flush();

            Response resp = readResponse();
            if (resp.code == 200 && checksum.equalsIgnoreCase(resp.headers.getOrDefault("Checksum", checksum))) {
                return resp.statusLine;
            }
        }
        throw new IOException("Failed to upload after retries");
    }

    public byte[] download(String fileName) throws IOException {
        String req = "GET /download?file=" + fileName + " HTTP/1.1\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        if (resp.code != 200) {
            throw new IOException("Server returned: " + resp.statusLine);
        }
        int length = Integer.parseInt(resp.headers.getOrDefault("Content-Length", "0"));
        byte[] buf = in.readNBytes(length);
        String checksum = resp.headers.get("Checksum");
        if (checksum != null && !checksum.equalsIgnoreCase(md5Hex(buf))) {
            throw new IOException("Checksum mismatch on download");
        }
        return buf;
    }

    public String delete(String fileName) throws IOException {
        String req = "DELETE /delete?file=" + fileName + " HTTP/1.1\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        return resp.statusLine;
    }

    public String listFiles() throws IOException {
        String req = "POST /listFiles HTTP/1.1\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        if (resp.code != 200) return null;
        int length = Integer.parseInt(resp.headers.getOrDefault("Content-Length", "0"));
        char[] buf = new char[length];
        int read = 0;
        while (read < length) {
            int r = in.read(buf, read, length - read);
            if (r == -1) break;
            read += r;
        }
        return new String(buf, 0, read);
    }

    public String sync() throws IOException {
        String req = "POST /sync HTTP/1.1\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        return resp.statusLine;
    }

    private Response readResponse() throws IOException {
        String statusLine = in.readLine();
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        int code = 0;
        if (statusLine != null && statusLine.startsWith("HTTP/1.1")) {
            String[] parts = statusLine.split(" ", 3);
            if (parts.length >= 2) {
                code = Integer.parseInt(parts[1]);
            }
        }
        Response r = new Response();
        r.statusLine = statusLine;
        r.code = code;
        r.headers = headers;
        return r;
    }

    private static class Response {
        String statusLine;
        int code;
        Map<String, String> headers;
    }

    private static String md5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
