package org.soprasteria.avans.lockercloud.syncserver.protocol;

import org.soprasteria.avans.lockercloud.service.FileManagerService;

import java.io.*;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic SSL socket server implementing the simple LockerCloud protocol.
 * This server is intentionally framework-free and uses Java SSL sockets only.
 */
public class ProtocolSyncServer implements Closeable {
    private final int port;
    private final FileManagerService fileService;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private SSLServerSocket serverSocket;
    private int actualPort;

    public ProtocolSyncServer(int port, FileManagerService fileService) {
        this.port = port;
        this.fileService = fileService;
    }

    /** Start the server and accept connections in a background thread using SSL. */
    public void start() throws IOException {
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) factory.createServerSocket(port);
        actualPort = serverSocket.getLocalPort();
        Thread acceptThread = new Thread(this::acceptLoop, "syncserver-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                pool.submit(() -> handleClient(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleClient(SSLSocket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()) {
            String greeting = reader.readLine();
            if ("HALLO SERVER".equalsIgnoreCase(greeting)) {
                out.write("HALLO CLIENT\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                // next line is the actual start line
                greeting = reader.readLine();
            }
            String startLine = greeting;
            if (startLine == null || startLine.isBlank()) {
                return;
            }
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }
            StringTokenizer st = new StringTokenizer(startLine);
            String method = st.nextToken();
            String path = st.nextToken();
            switch (method.toUpperCase()) {
                case "GET" -> handleDownload(path, out);
                case "POST" -> {
                    if (path.startsWith("/upload")) {
                        handleUpload(headers, reader, out);
                    } else if (path.startsWith("/listFiles")) {
                        handleList(out);
                    } else if (path.startsWith("/sync")) {
                        handleSync(out);
                    } else {
                        writeStatus(out, "400 Bad Request");
                    }
                }
                case "DELETE" -> handleDelete(path, out);
                default -> writeStatus(out, "400 Bad Request");
            }
        } catch (IOException e) {
            // ignore client abort
        }
    }

    private void handleDownload(String path, OutputStream out) throws IOException {
        int idx = path.indexOf("file=");
        if (idx < 0) {
            writeStatus(out, "400 Bad Request");
            return;
        }
        String fileName = path.substring(idx + 5);
        try {
            byte[] data = fileService.getFile(fileName);
            String checksum = fileService.calculateChecksum(data);
            writeStatus(out, "200 OK");
            writeHeader(out, "Content-Length", String.valueOf(data.length));
            writeHeader(out, "Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            writeHeader(out, "Checksum", checksum);
            endHeaders(out);
            out.write(data);
        } catch (Exception e) {
            writeStatus(out, "404 Not Found");
            endHeaders(out);
        }
    }

    private void handleUpload(Map<String, String> headers, BufferedReader reader, OutputStream out) throws IOException {
        String disposition = headers.getOrDefault("Content-Disposition", "");
        String fileName = "uploaded.bin";
        int fnIdx = disposition.indexOf("filename=");
        if (fnIdx >= 0) {
            fileName = disposition.substring(fnIdx + 9).replace("\"", "");
        }
        long length = Long.parseLong(headers.getOrDefault("Content-Length", "0"));
        Path chunkPath;
        Integer chunkIdx = headers.containsKey("Chunk-Index") ? Integer.parseInt(headers.get("Chunk-Index")) : null;
        if (chunkIdx != null) {
            chunkPath = getStorage().resolve(fileName + ".part" + chunkIdx);
        } else {
            chunkPath = getStorage().resolve(fileName + ".tmp");
        }

        try (OutputStream fos = Files.newOutputStream(chunkPath)) {
            long remaining = length;
            char[] buffer = new char[8192];
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int r = reader.read(buffer, 0, toRead);
                if (r == -1) break;
                fos.write(new String(buffer, 0, r).getBytes(StandardCharsets.ISO_8859_1));
                remaining -= r;
            }
        }

        if (chunkIdx == null) {
            byte[] data = Files.readAllBytes(chunkPath);
            String expected = headers.get("Checksum");
            String actual = fileService.calculateChecksum(data);
            if (expected != null && !expected.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(chunkPath);
                writeStatus(out, "409 Checksum Mismatch");
                endHeaders(out);
                return;
            }
            fileService.saveStream(fileName, new ByteArrayInputStream(data));
            Files.deleteIfExists(chunkPath);
        } else if (headers.get("Chunk-Total") != null && chunkIdx.equals(Integer.parseInt(headers.get("Chunk-Total")))) {
            assembleChunks(fileName, Integer.parseInt(headers.get("Chunk-Total")), headers.get("File-Checksum"));
        }

        writeStatus(out, "200 OK");
        endHeaders(out);
    }

    private void handleList(OutputStream out) throws IOException {
        writeStatus(out, "200 OK");
        StringBuilder sb = new StringBuilder();
        for (String name : fileService.listFiles()) {
            sb.append(name).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        writeHeader(out, "Content-Length", String.valueOf(bytes.length));
        endHeaders(out);
        out.write(bytes);
    }

    private void handleSync(OutputStream out) throws IOException {
        var result = fileService.performServerSideLocalSync();
        String payload = "UPLOAD:" + String.join(",", result.getFilesToUpload()) +
                "\nDOWNLOAD:" + String.join(",", result.getFilesToDownload());
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        writeStatus(out, "200 OK");
        writeHeader(out, "Content-Length", String.valueOf(bytes.length));
        endHeaders(out);
        out.write(bytes);
    }

    private void handleDelete(String path, OutputStream out) throws IOException {
        int idx = path.indexOf("file=");
        if (idx < 0) {
            writeStatus(out, "400 Bad Request");
            endHeaders(out);
            return;
        }
        String fileName = path.substring(idx + 5);
        try {
            fileService.deleteFile(fileName);
            writeStatus(out, "200 OK");
        } catch (Exception e) {
            writeStatus(out, "404 Not Found");
        }
        endHeaders(out);
    }

    private Path getStorage() {
        try {
            var field = FileManagerService.class.getDeclaredField("storageLocation");
            field.setAccessible(true);
            return (Path) field.get(fileService);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access storage location", e);
        }
    }

    private void assembleChunks(String fileName, int total, String finalChecksum) throws IOException {
        Path storage = getStorage();
        Path finalFile = storage.resolve(fileName);
        try (OutputStream out = Files.newOutputStream(finalFile)) {
            for (int i = 1; i <= total; i++) {
                Path part = storage.resolve(fileName + ".part" + i);
                Files.copy(part, out);
                Files.deleteIfExists(part);
            }
        }
        if (finalChecksum != null) {
            byte[] data = Files.readAllBytes(finalFile);
            String actual = fileService.calculateChecksum(data);
            if (!finalChecksum.equalsIgnoreCase(actual)) {
                Files.deleteIfExists(finalFile);
                throw new IOException("Final checksum mismatch");
            }
        }
    }

    private void writeStatus(OutputStream out, String status) throws IOException {
        out.write((status + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeHeader(OutputStream out, String name, String value) throws IOException {
        out.write((name + ": " + value + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private void endHeaders(OutputStream out) throws IOException {
        out.write("\n".getBytes(StandardCharsets.UTF_8));
    }

    public int getPort() {
        return actualPort;
    }

    @Override
    public void close() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        pool.shutdownNow();
    }
}
