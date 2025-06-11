package org.soprasteria.avans.lockercloud.syncserver.protocol;

import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Very small client for the LockerCloud protocol. Supports upload and download
 * operations for demonstration purposes.
 */
public class ProtocolSyncClient {
    private final String host;
    private final int port;

    public ProtocolSyncClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public byte[] download(String fileName) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             InputStream in = socket.getInputStream()) {
            writer.write("HALLO SERVER\n");
            writer.write("GET /download?file=" + fileName + "\n\n");
            writer.flush();
            Map<String, String> headers = readResponseHeaders(in);
            int len = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            byte[] data = in.readNBytes(len);
            return data;
        }
    }

    public void upload(String fileName, byte[] data) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             InputStream in = socket.getInputStream()) {
            writer.write("HALLO SERVER\n");
            writer.write("POST /upload\n");
            writer.write("Content-Length: " + data.length + "\n");
            writer.write("Content-Disposition: attachment; filename=\"" + fileName + "\"\n\n");
            writer.flush();
            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();
            readResponseHeaders(in); // ignore
        }
    }

    public String listFiles() throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             InputStream in = socket.getInputStream()) {
            writer.write("HALLO SERVER\n");
            writer.write("POST /listFiles\n\n");
            writer.flush();
            Map<String, String> headers = readResponseHeaders(in);
            int len = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            return new String(in.readNBytes(len), StandardCharsets.UTF_8);
        }
    }

    public void delete(String fileName) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             InputStream in = socket.getInputStream()) {
            writer.write("HALLO SERVER\n");
            writer.write("DELETE /delete?file=" + fileName + "\n\n");
            writer.flush();
            readResponseHeaders(in);
        }
    }

    public String sync() throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             InputStream in = socket.getInputStream()) {
            writer.write("HALLO SERVER\n");
            writer.write("POST /sync\n\n");
            writer.flush();
            Map<String, String> headers = readResponseHeaders(in);
            int len = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            return new String(in.readNBytes(len), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> readResponseHeaders(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        String status = reader.readLine();
        headers.put("Status", status);
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return headers;
    }
}
