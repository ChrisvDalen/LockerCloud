package org.soprasteria.avans.lockercloud.client;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;

/**
 * Simple command line client that communicates with the SSLFileServer over raw sockets.
 * Usage:
 *   java -cp LockerCloud.jar org.soprasteria.avans.lockercloud.client.SocketClient list
 *   java -cp LockerCloud.jar org.soprasteria.avans.lockercloud.client.SocketClient upload <path>
 *   java -cp LockerCloud.jar org.soprasteria.avans.lockercloud.client.SocketClient download <name> <dest>
 *   java -cp LockerCloud.jar org.soprasteria.avans.lockercloud.client.SocketClient delete <name>
 */
public class SocketClient {
    private static final String HOST = System.getProperty("server.host", "localhost");
    private static final int PORT = Integer.getInteger("socket.port", 8443);
    private static final String KEYSTORE = System.getProperty("client.keystore", "keystore.p12");
    private static final String PASSWORD = System.getProperty("client.keystorePassword", "YourPasswordHere");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        String cmd = args[0];
        switch (cmd) {
            case "list" -> listFiles();
            case "upload" -> {
                if (args.length != 2) { usage(); return; }
                upload(Path.of(args[1]));
            }
            case "download" -> {
                if (args.length != 3) { usage(); return; }
                download(args[1], Path.of(args[2]));
            }
            case "delete" -> {
                if (args.length != 2) { usage(); return; }
                delete(args[1]);
            }
            default -> usage();
        }
    }

    private static void usage() {
        System.out.println("Usage: SocketClient [list|upload <file>|download <name> <dest>|delete <name>]");
    }

    private static SSLSocket connect() throws Exception {
        char[] pass = PASSWORD.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(KEYSTORE)) {
            ks.load(is, pass);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pass);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLSocketFactory factory = ctx.getSocketFactory();
        return (SSLSocket) factory.createSocket(HOST, PORT);
    }

    private static void upload(Path path) throws Exception {
        byte[] data = Files.readAllBytes(path);
        try (SSLSocket socket = connect();
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.writeBytes("POST /upload HTTP/1.1\r\n");
            out.writeBytes("Host: " + HOST + "\r\n");
            out.writeBytes("Content-Length: " + data.length + "\r\n");
            out.writeBytes("Content-Disposition: attachment; filename=\"" + path.getFileName() + "\"\r\n\r\n");
            out.write(data);
            out.flush();
            System.out.println(in.readLine());
        }
    }

    private static void download(String name, Path dest) throws Exception {
        try (SSLSocket socket = connect();
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             InputStream in = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            out.writeBytes("GET /download?file=" + name + " HTTP/1.1\r\n\r\n");
            out.flush();
            String status = reader.readLine();
            if (!status.contains("200")) {
                System.out.println(status);
                return;
            }
            String line;
            int len = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    len = Integer.parseInt(line.substring(15).trim());
                }
            }
            byte[] data = in.readNBytes(len);
            Files.write(dest, data);
            System.out.println("Downloaded to " + dest);
        }
    }

    private static void listFiles() throws Exception {
        try (SSLSocket socket = connect();
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.writeBytes("POST /listFiles HTTP/1.1\r\nContent-Length: 0\r\n\r\n");
            out.flush();
            String status = in.readLine();
            if (!status.contains("200")) {
                System.out.println(status);
                return;
            }
            String line;
            int len = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    len = Integer.parseInt(line.substring(15).trim());
                }
            }
            char[] buf = new char[len];
            int read = in.read(buf);
            System.out.println(new String(buf, 0, read));
        }
    }

    private static void delete(String name) throws Exception {
        try (SSLSocket socket = connect();
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out.writeBytes("DELETE /delete?file=" + name + " HTTP/1.1\r\n\r\n");
            out.flush();
            System.out.println(in.readLine());
        }
    }
}
