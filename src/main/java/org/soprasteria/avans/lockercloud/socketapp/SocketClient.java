package org.soprasteria.avans.lockercloud.socketapp;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;

public class SocketClient {
    private final String host;
    private final int port;
    private final String token;
    private final SecretKeySpec keySpec;

    public SocketClient(String host, int port, String token, String aesKey) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.keySpec = aesKey == null || aesKey.isBlank() ? null : new SecretKeySpec(aesKey.getBytes(), "AES");
    }

    public void upload(File file) throws Exception {
        try (Socket s = new Socket(host, port);
             BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             FileInputStream fis = new FileInputStream(file)) {
            authenticate(out, in);
            out.write(("UPLOAD " + file.getName() + " " + file.length() + "\n").getBytes());
            OutputStream dataOut = out;
            if (keySpec != null) {
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                dataOut = new CipherOutputStream(out, cipher);
            }
            fis.transferTo(dataOut);
            out.flush();
            System.out.println(in.readLine());
        }
    }

    public void download(String name, File target) throws Exception {
        try (Socket s = new Socket(host, port);
             BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
             BufferedInputStream bin = new BufferedInputStream(s.getInputStream());
             FileOutputStream fos = new FileOutputStream(target)) {
            authenticate(out, new BufferedReader(new InputStreamReader(bin)));
            out.write(("DOWNLOAD " + name + "\n").getBytes());
            out.flush();
            String lenLine = readLine(bin);
            long size = Long.parseLong(lenLine.trim());
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

    private void authenticate(OutputStream out, BufferedReader in) throws IOException {
        if (token != null && !token.isEmpty()) {
            out.write(("AUTH " + token + "\n").getBytes());
            out.flush();
            in.readLine();
        }
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
