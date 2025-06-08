package org.soprasteria.avans.lockercloud.syncserver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Path;

// Utility for generating temporary keystores
import org.soprasteria.avans.lockercloud.syncserver.KeyStoreTestUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SslSyncServerTest {

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
    void connectionsRejectedAfterStop() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        SslSyncServer server = new SslSyncServer(0, executor);
        server.start();
        int port = server.getPort();

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket("localhost", port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.write("hello\n");
            writer.flush();
            assertEquals("ACK:hello", reader.readLine());
        }

        server.stop();
        assertFalse(server.isRunning());

        assertThrows(ConnectException.class, () -> {
            Socket s = factory.createSocket("localhost", port);
            s.close();
        });

        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
