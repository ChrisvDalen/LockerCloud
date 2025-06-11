package org.soprasteria.avans.lockercloud.socket;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        StringBuilder req = new StringBuilder();
        req.append("POST /upload HTTP/1.1\n");
        req.append("Content-Length: ").append(data.length).append('\n');
        req.append("Content-Disposition: form-data; filename=\"").append(fileName).append("\"\n");
        req.append('\n');
        out.write(req.toString().getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
        return readStatus();
    }

    public byte[] download(String fileName) throws IOException {
        String req = "GET /download?file=" + fileName + " HTTP/1.1\n\n";
        out.write(req.getBytes(StandardCharsets.UTF_8));
        out.flush();
        String status = in.readLine();
        if (!status.startsWith("200")) {
            throw new IOException("Server returned: " + status);
        }
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx), line.substring(idx + 1).trim());
            }
        }
        int length = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        byte[] buf = in.readNBytes(length);
        return buf;
    }

    private String readStatus() throws IOException {
        String status = in.readLine();
        // consume headers
        while (in.readLine() != null && !in.readLine().isEmpty()) {}
        return status;
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
