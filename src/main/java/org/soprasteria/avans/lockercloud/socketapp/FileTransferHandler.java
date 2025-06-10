package org.soprasteria.avans.lockercloud.socketapp;

import org.soprasteria.avans.lockercloud.service.FileManagerService;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles one client connection using a simple HTTP-like protocol.
 */
public class FileTransferHandler implements Runnable {
    private final Socket socket;
    private final FileManagerService service;
    private final SecretKeySpec keySpec;

    public FileTransferHandler(Socket socket, FileManagerService service, String aesKey) {
        this.socket = socket;
        this.service = service;
        this.keySpec = aesKey == null || aesKey.isBlank() ? null : new SecretKeySpec(aesKey.getBytes(), "AES");
    }

    @Override
    public void run() {
        try (socket) {
            handleConnection();
        } catch (IOException e) {
            // Ignore closed connection
        }
    }

    private void handleConnection() throws IOException {
        BufferedInputStream bin = new BufferedInputStream(socket.getInputStream());
        BufferedOutputStream bout = new BufferedOutputStream(socket.getOutputStream());
        String startLine = readLine(bin);
        if (startLine == null) return;
        Map<String, String> headers = readHeaders(bin);
        ProtocolHandler.RequestType type = ProtocolHandler.parseStartLine(startLine);
        switch (type) {
            case GET_DOWNLOAD -> handleDownload(startLine, bout);
            case POST_UPLOAD -> handleUpload(headers, bin, bout);
            case POST_LIST -> handleList(bout);
            case DELETE_FILE -> handleDelete(startLine, bout);
            default -> writeStatus(bout, "400 Bad Request");
        }
    }

    private void handleUpload(Map<String,String> headers, InputStream in, OutputStream out) throws IOException {
        String disposition = headers.getOrDefault("Content-Disposition", "");
        String fileName = extractFileName(disposition);
        long length = Long.parseLong(headers.getOrDefault("Content-Length", "0"));
        if (fileName == null || length <= 0) {
            writeStatus(out, "400 Bad Request");
            return;
        }
        InputStream dataIn = new LimitInputStream(in, length);
        if (keySpec != null) {
            try {
                dataIn = cipherStream(dataIn, Cipher.DECRYPT_MODE);
            } catch (GeneralSecurityException e) {
                writeStatus(out, "500 Internal Server Error");
                return;
            }
        }
        service.saveStream(fileName, dataIn);
        writeStatus(out, "200 OK");
    }

    private void handleDownload(String startLine, OutputStream out) throws IOException {
        String path = startLine.split(" ")[1];
        String fileName = ProtocolHandler.extractQueryParam(path, "file");
        if (fileName == null) { writeStatus(out, "400 Bad Request"); return; }
        byte[] data = service.getFile(fileName);
        if (keySpec != null) {
            try {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                ByteArrayOutputStream enc = new ByteArrayOutputStream();
                try (CipherOutputStream cos = new CipherOutputStream(enc, cipher)) {
                    cos.write(data);
                }
                data = enc.toByteArray();
            } catch (GeneralSecurityException e) {
                writeStatus(out, "500 Internal Server Error");
                return;
            }
        }
        writeHeaders(out, data.length, fileName, service.calculateChecksum(data));
        out.write(data);
    }

    private void handleList(OutputStream out) throws IOException {
        List<String> files = service.listFiles();
        StringBuilder sb = new StringBuilder();
        for (String f : files) sb.append(f).append("\n");
        byte[] bytes = sb.toString().getBytes();
        writeHeaders(out, bytes.length, null, null);
        out.write(bytes);
    }

    private void handleDelete(String startLine, OutputStream out) throws IOException {
        String path = startLine.split(" ")[1];
        String fileName = ProtocolHandler.extractQueryParam(path, "file");
        if (fileName == null) { writeStatus(out, "400 Bad Request"); return; }
        service.deleteFile(fileName);
        writeStatus(out, "200 OK");
    }

    private Map<String,String> readHeaders(InputStream in) throws IOException {
        Map<String,String> map = new HashMap<>();
        String line;
        while (!"".equals(line = readLine(in))) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx+1).trim();
                map.put(key, val);
            }
        }
        return map;
    }

    private void writeStatus(OutputStream out, String status) throws IOException {
        out.write(("HTTP/1.1 " + status + "\r\n\r\n").getBytes());
        out.flush();
    }

    private void writeHeaders(OutputStream out, long len, String name, String checksum) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write(("Content-Length: " + len + "\r\n").getBytes());
        if (name != null) {
            out.write(("Content-Disposition: attachment; filename=\"" + name + "\"\r\n").getBytes());
        }
        if (checksum != null) {
            out.write(("Checksum: " + checksum + "\r\n").getBytes());
        }
        out.write("\r\n".getBytes());
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') baos.write(b);
        }
        if (baos.size() == 0 && b == -1) return null;
        return baos.toString();
    }

    private InputStream cipherStream(InputStream in, int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, keySpec);
        return new CipherInputStream(in, cipher);
    }

    private static class LimitInputStream extends FilterInputStream {
        private long left;
        protected LimitInputStream(InputStream in, long size) { super(in); this.left = size; }
        @Override public int read() throws IOException { if (left <= 0) return -1; int b = super.read(); if (b != -1) left--; return b; }
        @Override public int read(byte[] b, int off, int len) throws IOException { if (left <= 0) return -1; len = (int)Math.min(len, left); int r = super.read(b, off, len); if (r != -1) left -= r; return r; }
    }

    private String extractFileName(String disposition) {
        if (disposition == null) return null;
        for (String part : disposition.split(";")) {
            part = part.trim();
            if (part.startsWith("filename=")) {
                String v = part.substring(9);
                if (v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length()-1);
                }
                return v;
            }
        }
        return null;
    }
}
