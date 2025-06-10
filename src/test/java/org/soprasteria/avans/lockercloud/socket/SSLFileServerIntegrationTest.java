package org.soprasteria.avans.lockercloud.socket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.soprasteria.avans.lockercloud.syncserver.KeyStoreTestUtils;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class SSLFileServerIntegrationTest {

    private static Path KEYSTORE;

    @BeforeAll
    static void setup() throws Exception {
        KEYSTORE = KeyStoreTestUtils.createTempKeyStore("password");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
    }

    @AfterAll
    static void cleanup() {
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
    }

    @Test
    void uploadDownloadListDelete() throws Exception {
        FileManagerService service = new FileManagerService();
        SSLFileServer server = new SSLFileServer(0, service,
                KEYSTORE.toAbsolutePath().toString(), "password");
        Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();

        // wait for server socket to open
        Thread.sleep(500);
        int port = service.listFiles().size(); // dummy to load class
        port = getServerPort(server); // reflection trick

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            byte[] data = "hello".getBytes();
            out.writeBytes("UPLOAD test.txt " + data.length + "\n");
            out.write(data);
            out.flush();
            assertEquals("OK", readLine(in));
        }

        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeBytes("LIST\n");
            assertEquals("test.txt", readLine(in));
            assertEquals("END", readLine(in));
        }

        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeBytes("DOWNLOAD test.txt\n");
            int len = Integer.parseInt(readLine(in));
            byte[] buf = new byte[len];
            in.readFully(buf);
            assertArrayEquals("hello".getBytes(), buf);
        }

        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeBytes("DELETE test.txt\n");
            assertEquals("OK", readLine(in));
        }
    }

    private static String readLine(DataInputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            baos.write(b);
        }
        return baos.toString("UTF-8");
    }

    private int getServerPort(SSLFileServer server) throws Exception {
        java.lang.reflect.Field portField = SSLFileServer.class.getDeclaredField("port");
        portField.setAccessible(true);
        return portField.getInt(server);
    }
}
