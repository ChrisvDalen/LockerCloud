package org.soprasteria.avans.lockercloud.socket;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal socket server implementing the HTTP-inspired protocol for file
 * synchronisation. It supports upload, download, delete, listFiles and sync
 * commands without using a web framework.
 */
public class SocketFileServer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SocketFileServer.class);
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
            log.info("SocketFileServer started on port {}", port);
            while (running) {
                Socket socket = serverSocket.accept();
                log.debug("Accepted connection from {}", socket.getRemoteSocketAddress());
                socket.setReceiveBufferSize(64 * 1024);
                socket.setSendBufferSize(64 * 1024);
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            log.error("Socket server stopped", e);
        }
    }

    private void handleClient(Socket socket) {
        try {
            InputStream rawIn = new BufferedInputStream(socket.getInputStream(), 64 * 1024);
            BufferedReader reader = new BufferedReader(new InputStreamReader(rawIn, StandardCharsets.UTF_8));
            OutputStream out = new BufferedOutputStream(socket.getOutputStream(), 64 * 1024);

            String startLine = reader.readLine();
            if (startLine == null) return;
            log.debug("Request: {}", startLine);
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }

            if (startLine.startsWith("POST /upload")) {
                handleUpload(rawIn, out, headers);
            } else if (startLine.startsWith("GET /download")) {
                handleDownload(out, startLine);
            } else if (startLine.startsWith("DELETE /delete")) {
                handleDelete(out, startLine);
            } else if (startLine.startsWith("POST /listFiles")) {
                handleListFiles(out);
            } else if (startLine.startsWith("POST /sync")) {
                handleSync(out);
            } else {
                writeStatus(out, 400, "Bad Request", "Unknown command");
            }
        } catch (IOException e) {
            log.error("Client handling error", e);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleUpload(InputStream in, OutputStream out, Map<String, String> headers) throws IOException {
        String lengthStr = headers.get("Content-Length");
        String disposition = headers.get("Content-Disposition");
        String checksumHeader = headers.get("Checksum");
        if (lengthStr == null || disposition == null) {
            writeStatus(out, 400, "Bad Request", "Missing headers");
            return;
        }
        long length = Long.parseLong(lengthStr);
        String fileName = extractFileName(disposition);
        if (fileName == null) {
            writeStatus(out, 400, "Bad Request", "No filename");
            return;
        }
        try {
            log.info("Uploading {} ({} bytes)", fileName, length);
            String actual = fileManager.saveFileStream(fileName, in, length, checksumHeader);
            writeStatus(out, 200, "OK", "uploaded");
            out.write(("Checksum: " + actual + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            log.debug("Upload complete for {}", fileName);
        } catch (Exception e) {
            log.error("Upload failed for {}", fileName, e);
            writeStatus(out, 500, "Internal Server Error", e.getMessage());
        }
    }

    private void handleDownload(OutputStream out, String startLine) throws IOException {
        int idx = startLine.indexOf("file=");
        if (idx == -1) {
            writeStatus(out, 400, "Bad Request", "Missing file parameter");
            return;
        }
        String fileName = startLine.substring(idx + 5).trim();
        int spaceIdx = fileName.indexOf(' ');
        if (spaceIdx > 0) {
            fileName = fileName.substring(0, spaceIdx);
        }
        try {
            log.info("Downloading {}", fileName);
            byte[] data = fileManager.getFile(fileName);
            if (data == null) {
                writeStatus(out, 404, "Not Found", "no file");
                return;
            }
            String checksum = fileManager.getFileChecksum(fileName);
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 200 OK\r\n");
            sb.append("Content-Length: ").append(data.length).append("\r\n");
            sb.append("Content-Disposition: attachment; filename=\"").append(fileName).append("\"\r\n");
            if (checksum != null) sb.append("Checksum: ").append(checksum).append("\r\n");
            sb.append("\r\n");
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.write(data);
            out.flush();
            log.debug("Download complete for {}", fileName);
        } catch (Exception e) {
            log.error("Download failed for {}", fileName, e);
            writeStatus(out, 500, "Internal Server Error", e.getMessage());
        }
    }

    private void handleDelete(OutputStream out, String startLine) throws IOException {
        int idx = startLine.indexOf("file=");
        if (idx == -1) {
            writeStatus(out, 400, "Bad Request", "Missing file parameter");
            return;
        }
        String fileName = startLine.substring(idx + 5).trim();
        int spaceIdx = fileName.indexOf(' ');
        if (spaceIdx > 0) {
            fileName = fileName.substring(0, spaceIdx);
        }
        try {
            log.info("Deleting {}", fileName);
            fileManager.deleteFile(fileName);
            writeStatus(out, 200, "OK", "deleted");
        } catch (Exception e) {
            log.error("Delete failed for {}", fileName, e);
            writeStatus(out, 500, "Internal Server Error", e.getMessage());
        }
    }

    private void handleListFiles(OutputStream out) throws IOException {
        try {
            log.info("Listing files");
            List<String> files = fileManager.listFiles();
            String body = String.join("\n", files);
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 200 OK\r\n");
            sb.append("Content-Length: ").append(body.getBytes(StandardCharsets.UTF_8).length).append("\r\n\r\n");
            sb.append(body);
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            log.debug("Returned {} file names", files.size());
        } catch (Exception e) {
            log.error("Listing files failed", e);
            writeStatus(out, 500, "Internal Server Error", e.getMessage());
        }
    }

    private void handleSync(OutputStream out) throws IOException {
        try {
            log.info("Sync requested");
            SyncResult result = fileManager.performServerSideLocalSync();
            String msg = "sync completed";
            if (result != null) {
                msg = String.format("upload=%d, download=%d, conflicts=%d",
                        result.getUploadCount(), result.getDownloadCount(), result.getConflictCount());
            }
            writeStatus(out, 200, "OK", msg);
        } catch (Exception e) {
            log.error("Sync failed", e);
            writeStatus(out, 500, "Internal Server Error", e.getMessage());
        }
    }

    private void writeStatus(OutputStream out, int code, String text, String msg) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(code).append(' ').append(text).append("\r\n");
        if (msg != null && !msg.isEmpty()) {
            sb.append("Message: ").append(msg).append("\r\n");
        }
        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
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
