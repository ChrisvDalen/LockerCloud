package org.soprasteria.avans.lockercloud.protocol;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.soprasteria.avans.lockercloud.syncserver.protocol.ProtocolSyncClient;
import org.soprasteria.avans.lockercloud.syncserver.protocol.ProtocolSyncServer;
import org.soprasteria.avans.lockercloud.syncserver.KeyStoreTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Basic integration test for ProtocolSyncServer and ProtocolSyncClient. */
public class ProtocolSyncServerTest {
    private static Path KEYSTORE;
    private ProtocolSyncServer server;
    private FileManagerService service;
    private Path storage;

    @BeforeEach
    void setup() throws Exception {
        KEYSTORE = KeyStoreTestUtils.createTempKeyStore("password");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", KEYSTORE.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        service = new FileManagerService();
        // override storage directory
        java.lang.reflect.Field storageField = FileManagerService.class.getDeclaredField("storageLocation");
        storageField.setAccessible(true);
        storage = Files.createTempDirectory("store");
        storageField.set(service, storage);
        server = new ProtocolSyncServer(0, service);
        server.start();
        // wait a moment for the server
        TimeUnit.MILLISECONDS.sleep(100);
    }

    @AfterEach
    void cleanup() throws Exception {
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        server.close();
        Files.walk(storage)
                .map(Path::toFile)
                .sorted((a,b) -> -a.compareTo(b))
                .forEach(java.io.File::delete);
        Files.deleteIfExists(KEYSTORE);
    }

    @Test
    void uploadThenDownload() throws Exception {
        int port = server.getPort();
        ProtocolSyncClient client = new ProtocolSyncClient("localhost", port);
        byte[] data = "hello".getBytes();
        client.upload("test.txt", data);
        byte[] received = client.download("test.txt");
        assertArrayEquals(data, received);
    }
}
