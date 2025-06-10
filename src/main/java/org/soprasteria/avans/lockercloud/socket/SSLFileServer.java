package org.soprasteria.avans.lockercloud.socket;

import org.soprasteria.avans.lockercloud.service.FileManagerService;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.List;

/**
 * Simple SSL socket based server for file CRUD operations.
 * This is not intended for production use but demonstrates how the
 * application can expose its file API over raw SSL sockets.
 */
public class SSLFileServer implements Runnable {

    private final int port;
    private final FileManagerService fileService;
    private final String keyStorePath;
    private final String keyStorePassword;

    public SSLFileServer(int port, FileManagerService fileService,
                         String keyStorePath, String keyStorePassword) {
        this.port = port;
        this.fileService = fileService;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
    }

    @Override
    public void run() {
        try {
            SSLContext ctx = createContext();
            SSLServerSocketFactory factory = ctx.getServerSocketFactory();
            try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
                while (true) {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    handle(socket);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("SSL socket server failed", e);
        }
    }

    private SSLContext createContext() throws Exception {
        char[] pass = keyStorePassword.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(keyStorePath)) {
            ks.load(is, pass);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pass);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ctx;
    }

    private void handle(SSLSocket socket) {
        try {
            InputStream rawIn = socket.getInputStream();
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(rawIn));

            String start = reader.readLine();
            if (start == null) {
                return;
            }

            String[] first = start.split(" ");
            if (first.length < 2) {
                return;
            }
            String method = first[0];
            String target = first[1];

            // Read headers
            String line;
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }

            int contentLen = headers.containsKey("Content-Length") ? Integer.parseInt(headers.get("Content-Length")) : 0;
            byte[] body = new byte[contentLen];
            int read = 0;
            while (read < contentLen) {
                int r = rawIn.read(body, read, contentLen - read);
                if (r == -1) break;
                read += r;
            }

            switch (method) {
                case "POST" -> handlePost(target, headers, body, out);
                case "GET" -> handleGet(target, out);
                case "DELETE" -> handleDelete(target, out);
                default -> out.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
            }

            out.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    private void handlePost(String target, java.util.Map<String, String> headers, byte[] body, DataOutputStream out) throws IOException {
        if ("/upload".equals(target)) {
            String disposition = headers.getOrDefault("Content-Disposition", "");
            String fileName = disposition.replace("attachment; filename=", "").replace("\"", "");
            try (InputStream is = new ByteArrayInputStream(body)) {
                fileService.saveStream(fileName, is);
            }
            out.writeBytes("HTTP/1.1 200 OK\r\n\r\n");
        } else if ("/listFiles".equals(target)) {
            java.util.List<String> list = fileService.listFiles();
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
            out.writeBytes("HTTP/1.1 200 OK\r\nContent-Length: " + json.getBytes().length + "\r\n\r\n");
            out.writeBytes(json);
        } else if ("/sync".equals(target)) {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<org.soprasteria.avans.lockercloud.model.FileMetadata> meta = java.util.Arrays.asList(mapper.readValue(body, org.soprasteria.avans.lockercloud.model.FileMetadata[].class));
            org.soprasteria.avans.lockercloud.dto.SyncResult result = fileService.syncFiles(meta);
            String json = mapper.writeValueAsString(result);
            out.writeBytes("HTTP/1.1 200 OK\r\nContent-Length: " + json.getBytes().length + "\r\n\r\n");
            out.writeBytes(json);
        } else {
            out.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
        }
    }

    private void handleGet(String target, DataOutputStream out) throws IOException {
        if (target.startsWith("/download")) {
            int idx = target.indexOf("file=");
            if (idx == -1) {
                out.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
                return;
            }
            String name = target.substring(idx + 5);
            byte[] data = fileService.getFile(name);
            String checksum = fileService.calculateChecksum(data);
            out.writeBytes("HTTP/1.1 200 OK\r\n");
            out.writeBytes("Content-Length: " + data.length + "\r\n");
            out.writeBytes("Checksum: " + checksum + "\r\n\r\n");
            out.write(data);
        } else {
            out.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
        }
    }

    private void handleDelete(String target, DataOutputStream out) throws IOException {
        if (target.startsWith("/delete")) {
            int idx = target.indexOf("file=");
            if (idx == -1) {
                out.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
                return;
            }
            String name = target.substring(idx + 5);
            fileService.deleteFile(name);
            out.writeBytes("HTTP/1.1 200 OK\r\n\r\n");
        } else {
            out.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
        }
    }
}
