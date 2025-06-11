package org.soprasteria.avans.lockercloud.socket;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple socket server implementing the custom file upload/download protocol.
 * This server is intentionally minimal to satisfy the requirement of using
 * raw sockets instead of a web framework.
 */
public class SocketFileServer implements Runnable {
    private final int port;
    private final FileManagerService fileManager;
    private volatile boolean running = true;

    public SocketFileServer(int port, FileManagerService fileManager) {
        this.port = port;
        this.fileManager = fileManager;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Socket server stopped: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {

            String startLine = reader.readLine();
            if (startLine == null) return;
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }

            if (startLine.startsWith("POST /upload")) {
                handleUpload(reader, out, headers);
            } else if (startLine.startsWith("GET /download")) {
                handleDownload(out, startLine, headers);
            } else {
                writeStatus(out, "400 Bad Request", "Unknown command");
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleUpload(BufferedReader reader, OutputStream out, Map<String, String> headers) throws IOException {
        String lengthStr = headers.get("Content-Length");
        String disposition = headers.get("Content-Disposition");
        if (lengthStr == null || disposition == null) {
            writeStatus(out, "400 Bad Request", "Missing headers");
            return;
        }
        long length = Long.parseLong(lengthStr);
        String fileName = extractFileName(disposition);
        if (fileName == null) {
            writeStatus(out, "400 Bad Request", "No filename");
            return;
        }
        char[] buf = new char[(int) length];
        int read = 0;
        while (read < length) {
            int r = reader.read(buf, read, (int) (length - read));
            if (r == -1) break;
            read += r;
        }
        byte[] data = new String(buf, 0, read).getBytes(StandardCharsets.ISO_8859_1);
        try {
            fileManager.saveFileBytes(fileName, data);
            writeStatus(out, "200 OK", "uploaded");
        } catch (Exception e) {
            writeStatus(out, "500 Internal Server Error", e.getMessage());
        }
    }

    private void handleDownload(OutputStream out, String startLine, Map<String, String> headers) throws IOException {
        int idx = startLine.indexOf("file=");
        if (idx == -1) {
            writeStatus(out, "400 Bad Request", "Missing file parameter");
            return;
        }
        String fileName = startLine.substring(idx + 5).trim();
        try {
            byte[] data = fileManager.getFile(fileName);
            if (data == null) {
                writeStatus(out, "404 Not Found", "no file");
                return;
            }
            String checksum = fileManager.getFileChecksum(fileName);
            StringBuilder sb = new StringBuilder();
            sb.append("200 OK\n");
            sb.append("Content-Length: ").append(data.length).append('\n');
            sb.append("Content-Disposition: attachment; filename=\"").append(fileName).append("\"\n");
            if (checksum != null) sb.append("Checksum: ").append(checksum).append('\n');
            sb.append('\n');
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.write(data);
        } catch (Exception e) {
            writeStatus(out, "500 Internal Server Error", e.getMessage());
        }
    }

    private void writeStatus(OutputStream out, String status, String msg) throws IOException {
        String resp = status + "\n" + "Message: " + msg + "\n\n";
        out.write(resp.getBytes(StandardCharsets.UTF_8));
    }

    private String extractFileName(String disposition) {
        for (String part : disposition.split(";")) {
            part = part.trim();
            if (part.startsWith("filename=")) {
                String name = part.substring("filename=".length());
                if (name.startsWith("\"") && name.endsWith("\"")) {
                    name = name.substring(1, name.length() - 1);
                }
                return name;
            }
        }
        return null;
    }
}
