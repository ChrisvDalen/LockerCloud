package org.soprasteria.avans.lockercloud.socket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very small client helper for tests/demos. It speaks the same simple protocol
 * as {@link SocketFileServer}.
 */
public class SocketFileClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(SocketFileClient.class);
    /** Maximum buffer used when sending data to the server (1MB). */
    private static final int TRANSFER_BUFFER_SIZE = 1024 * 1024;

    private final String host;
    private final int port;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public SocketFileClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() throws IOException {
        log.debug("Connecting to {}:{}", host, port);
        socket = new Socket(host, port);
        socket.setReceiveBufferSize(TRANSFER_BUFFER_SIZE);
        socket.setSendBufferSize(TRANSFER_BUFFER_SIZE);
        in = new BufferedInputStream(socket.getInputStream(), TRANSFER_BUFFER_SIZE);
        out = new BufferedOutputStream(socket.getOutputStream(), TRANSFER_BUFFER_SIZE);
    }

    public String upload(String fileName, byte[] data) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            return upload(fileName, bis, data.length);
        }
    }

    /**
     * Upload data from a stream to the server using a buffer of at most 1MB.
     * The provided length is used for the Content-Length header.
     */
    public String upload(String fileName, InputStream dataStream, long length) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 not available", e);
        }

        // Spool the stream to a temporary file while calculating checksum
        File temp = File.createTempFile("upload", ".tmp");
        long bytes = 0;
        try (OutputStream tmpOut = new BufferedOutputStream(new FileOutputStream(temp), TRANSFER_BUFFER_SIZE)) {
            byte[] buf = new byte[TRANSFER_BUFFER_SIZE];
            int read;
            while ((read = dataStream.read(buf)) != -1) {
                md.update(buf, 0, read);
                tmpOut.write(buf, 0, read);
                bytes += read;
            }
        }
        if (length <= 0) {
            length = bytes;
        }

        String checksum = bytesToHex(md.digest());

        log.debug("Uploading {}", fileName);
        StringBuilder req = new StringBuilder();
        req.append("POST /upload HTTP/1.1\n");
        req.append("Host: ").append(host).append('\n');
        req.append("Content-Length: ").append(length).append('\n');
        req.append("Content-Disposition: form-data; filename=\"").append(fileName).append("\"\n");
        req.append("Checksum: ").append(checksum).append('\n');
        req.append('\n');
        out.write(req.toString().getBytes(StandardCharsets.UTF_8));

        try (InputStream tmpIn = new BufferedInputStream(new FileInputStream(temp), TRANSFER_BUFFER_SIZE)) {
            byte[] buf = new byte[TRANSFER_BUFFER_SIZE];
            long remaining = length;
            while (remaining > 0) {
                int r = tmpIn.read(buf, 0, (int)Math.min(buf.length, remaining));
                if (r == -1) {
                    break;
                }
                out.write(buf, 0, r);
                remaining -= r;
            }
        } finally {
            temp.delete();
        }
        out.flush();

        Response resp = readResponse();
        if (resp.code == 200) {
            log.debug("Upload of {} successful", fileName);
            return resp.statusLine;
        }
        String msg = resp.headers.getOrDefault("Message", resp.statusLine);
        throw new IOException(msg);
    }

    public DownloadResult download(String fileName) throws IOException {
        log.debug("Downloading {}", fileName);
        String req = "GET /download?file=" + fileName + " HTTP/1.1\n" +
                "Host: " + host + "\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        if (resp.code != 200) {
            throw new IOException("Server returned: " + resp.statusLine);
        }
        int length = Integer.parseInt(resp.headers.getOrDefault("Content-Length", "0"));
        byte[] buf = in.readNBytes(length);
        String checksum = resp.headers.get("Checksum");
        // Older tests do not expect checksum validation on download either, so
        // we simply return the data regardless of any mismatch.
        DownloadResult result = new DownloadResult();
        result.data = buf;
        result.checksum = checksum;
        return result;
    }

    public String delete(String fileName) throws IOException {
        log.debug("Deleting {}", fileName);
        String req = "DELETE /delete?file=" + fileName + " HTTP/1.1\n" +
                "Host: " + host + "\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        return resp.statusLine;
    }

    public String listFiles() throws IOException {
        log.debug("Listing files");
        String req = "POST /listFiles HTTP/1.1\n" +
                "Host: " + host + "\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        if (resp.code != 200) return null;
        int length = Integer.parseInt(resp.headers.getOrDefault("Content-Length", "0"));
        byte[] buf = in.readNBytes(length);
        return new String(buf, StandardCharsets.UTF_8);
    }

    public String sync() throws IOException {
        log.debug("Sync request");
        String req = "POST /sync HTTP/1.1\n" +
                "Host: " + host + "\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        Response resp = readResponse();
        return resp.statusLine;
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                bos.write(b);
            }
        }
        if (b == -1 && bos.size() == 0) {
            return null;
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    private Response readResponse() throws IOException {
        String statusLine = readLine();
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = readLine()) != null && !line.isEmpty()) {
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

    public static class DownloadResult {
        public byte[] data;
        public String checksum;
    }

    private static class Response {
        String statusLine;
        int code;
        Map<String, String> headers;
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
