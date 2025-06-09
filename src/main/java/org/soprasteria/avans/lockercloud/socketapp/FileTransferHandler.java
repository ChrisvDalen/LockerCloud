package org.soprasteria.avans.lockercloud.socketapp;

import org.soprasteria.avans.lockercloud.service.FileManagerService;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.List;

public class FileTransferHandler implements Runnable {
    private final Socket socket;
    private final FileManagerService service;
    private final String token;
    private final SecretKeySpec keySpec;

    public FileTransferHandler(Socket socket, FileManagerService service, String token, String aesKey) {
        this.socket = socket;
        this.service = service;
        this.token = token;
        this.keySpec = aesKey == null || aesKey.isBlank() ? null : new SecretKeySpec(aesKey.getBytes(), "AES");
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream bout = new BufferedOutputStream(socket.getOutputStream())) {
            boolean authed = token.isEmpty();
            String line;
            while ((line = reader.readLine()) != null) {
                ProtocolHandler.CommandType type = ProtocolHandler.parseCommand(line);
                if (!authed && type != ProtocolHandler.CommandType.AUTH) {
                    bout.write("ERR Not Authenticated\n".getBytes());
                    bout.flush();
                    break;
                }
                switch (type) {
                    case AUTH -> {
                        String provided = line.substring("AUTH".length()).trim();
                        if (provided.equals(token)) {
                            authed = true;
                            bout.write("OK\n".getBytes());
                        } else {
                            bout.write("ERR BadToken\n".getBytes());
                            return;
                        }
                    }
                    case UPLOAD -> handleUpload(line, reader, bout);
                    case DOWNLOAD -> handleDownload(line, bout);
                    case LIST -> handleList(bout);
                    case DELETE -> handleDelete(line, bout);
                    default -> bout.write("ERR Unknown\n".getBytes());
                }
                bout.flush();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void handleUpload(String line, BufferedReader reader, OutputStream out) throws IOException {
        String[] p = line.split(" ", 3);
        if (p.length < 3) {
            out.write("ERR\n".getBytes());
            return;
        }
        String name = p[1];
        long size = Long.parseLong(p[2]);
        try {
            InputStream in = socket.getInputStream();
            InputStream dataIn = new LimitInputStream(in, size);
            if (keySpec != null) {
                dataIn = cipherStream(dataIn, Cipher.DECRYPT_MODE);
            }
            service.saveStream(name, dataIn);
            out.write("OK\n".getBytes());
        } catch (GeneralSecurityException e) {
            out.write("ERR\n".getBytes());
        }
    }

    private void handleDownload(String line, OutputStream out) throws IOException {
        String[] p = line.split(" ", 2);
        if (p.length < 2) { out.write("ERR\n".getBytes()); return; }
        String name = p[1];
        byte[] data = service.getFile(name);
        out.write((data.length + "\n").getBytes());
        InputStream in = new ByteArrayInputStream(data);
        if (keySpec != null) {
            try {
                in = cipherStream(in, Cipher.ENCRYPT_MODE);
            } catch (GeneralSecurityException e) {
                out.write("ERR\n".getBytes());
                return;
            }
        }
        in.transferTo(out);
    }

    private void handleList(OutputStream out) throws IOException {
        List<String> files = service.listFiles();
        for (String f : files) {
            out.write((f + "\n").getBytes());
        }
        out.write("END\n".getBytes());
    }

    private void handleDelete(String line, OutputStream out) throws IOException {
        String[] p = line.split(" ", 2);
        if (p.length < 2) { out.write("ERR\n".getBytes()); return; }
        service.deleteFile(p[1]);
        out.write("OK\n".getBytes());
    }

    private InputStream cipherStream(InputStream in, int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, keySpec);
        return new CipherInputStream(in, cipher);
    }

    private static class LimitInputStream extends FilterInputStream {
        private long left;
        protected LimitInputStream(InputStream in, long size) { super(in); this.left = size; }
        @Override public int read() throws IOException {
            if (left <= 0) return -1; int b = super.read(); if (b != -1) left--; return b; }
        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (left <= 0) return -1; len = (int)Math.min(len, left); int r = super.read(b, off, len); if (r != -1) left -= r; return r; }
    }
}
