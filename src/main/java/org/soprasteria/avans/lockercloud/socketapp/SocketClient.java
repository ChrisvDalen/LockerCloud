package org.soprasteria.avans.lockercloud.socketapp;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;

public class SocketClient {
    private final String host;
    private final int port;
    private final SecretKeySpec keySpec;

    public SocketClient(String host, int port, String aesKey) {
        this.host = host;
        this.port = port;
        this.keySpec = aesKey == null || aesKey.isBlank() ? null : new SecretKeySpec(aesKey.getBytes(), "AES");
    }

    public void upload(File file) throws Exception {
        try (Socket s = new Socket(host, port);
             BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
             BufferedInputStream in = new BufferedInputStream(s.getInputStream());
             FileInputStream fis = new FileInputStream(file)) {
            out.write("POST /upload HTTP/1.1\r\n".getBytes());
            out.write(("Host: " + host + "\r\n").getBytes());
            out.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            out.write("\r\n".getBytes());
            OutputStream dataOut = out;
            if (keySpec != null) {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                dataOut = new CipherOutputStream(out, cipher);
            }
            fis.transferTo(dataOut);
            out.flush();
            System.out.println(readLine(in));
        }
    }

    public void download(String name, File target) throws Exception {
        try (Socket s = new Socket(host, port);
             BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
             BufferedInputStream bin = new BufferedInputStream(s.getInputStream());
             FileOutputStream fos = new FileOutputStream(target)) {
            out.write(("GET /download?file=" + name + " HTTP/1.1\r\n").getBytes());
            out.write(("Host: " + host + "\r\n\r\n").getBytes());
            out.flush();
            String status = readLine(bin);
            if (!status.contains("200")) return;
            Map<String,String> headers = readHeaders(bin);
            long size = Long.parseLong(headers.getOrDefault("Content-Length", "0"));
            InputStream dataIn = bin;
            if (keySpec != null) {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
                dataIn = new CipherInputStream(bin, cipher);
            }
            long remaining = size;
            byte[] buf = new byte[8192];
            while (remaining > 0) {
                int r = dataIn.read(buf, 0, (int) Math.min(buf.length, remaining));
                if (r == -1) break;
                fos.write(buf, 0, r);
                remaining -= r;
            }
        }
    }

    private Map<String,String> readHeaders(InputStream in) throws IOException {
        Map<String,String> map = new HashMap<>();
        String line;
        while (!"".equals(line = readLine(in))) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                map.put(line.substring(0, idx).trim(), line.substring(idx+1).trim());
            }
        }
        return map;
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            baos.write(b);
        }
        return baos.toString();
    }
}
