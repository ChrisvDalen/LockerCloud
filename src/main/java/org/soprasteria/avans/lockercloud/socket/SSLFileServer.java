package org.soprasteria.avans.lockercloud.socket;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(SSLFileServer.class);

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
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            String cmd = readLine(in);
            if (cmd == null) return;
            logger.info("Received command: {}", cmd);
            String[] parts = cmd.split(" ");
            switch (parts[0].toUpperCase()) {
                case "UPLOAD":
                    handleUpload(parts, in, out);
                    break;
                case "DOWNLOAD":
                    handleDownload(parts, out);
                    break;
                case "DELETE":
                    handleDelete(parts, out);
                    break;
                case "LIST":
                    handleList(out);
                    break;
                default:
                    out.write("ERR\n".getBytes());
            }
            out.flush();
        } catch (IOException e) {
            logger.error("Socket error: {}", e.getMessage());
        }
    }

    private void handleUpload(String[] parts, DataInputStream in, DataOutputStream out) throws IOException {
        if (parts.length < 3) {
            out.write("ERR\n".getBytes());
            return;
        }
        String name = parts[1];
        long len = Long.parseLong(parts[2]);
        byte[] data = new byte[(int) len];
        in.readFully(data);
        try (InputStream dataIn = new ByteArrayInputStream(data)) {
            fileService.saveStream(name, dataIn);
            logger.info("Uploaded {} ({} bytes)", name, len);
        }
        out.writeBytes("OK\n");
    }

    private void handleDownload(String[] parts, DataOutputStream out) throws IOException {
        if (parts.length < 2) {
            out.writeBytes("ERR\n");
            return;
        }
        String name = parts[1];
        byte[] data = fileService.getFile(name);
        out.writeBytes(data.length + "\n");
        out.write(data);
        logger.info("Downloaded {} ({} bytes)", name, data.length);
    }

    private void handleDelete(String[] parts, DataOutputStream out) throws IOException {
        if (parts.length < 2) {
            out.writeBytes("ERR\n");
            return;
        }
        fileService.deleteFile(parts[1]);
        logger.info("Deleted {}", parts[1]);
        out.writeBytes("OK\n");
    }

    private void handleList(DataOutputStream out) throws IOException {
        List<String> files = fileService.listFiles();
        for (String f : files) {
            out.writeBytes(f + "\n");
        }
        out.writeBytes("END\n");
        logger.info("Listed {} files", files.size());
    }

    private String readLine(DataInputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            baos.write(b);
        }
        if (baos.size() == 0 && b == -1) {
            return null;
        }
        return baos.toString("UTF-8");
    }
}
