package org.soprasteria.avans.lockercloud.syncserver;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.soprasteria.avans.lockercloud.syncserver.KeyStoreTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class ServerBootstrapIT {

    @Test
    void shutdownHookCleansResourcesOnSigInt() throws Exception {
        Path keyStore = KeyStoreTestUtils.createTempKeyStore("password");
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Djavax.net.ssl.keyStore=" + keyStore.toAbsolutePath(),
                "-Djavax.net.ssl.keyStorePassword=password",
                "-Djavax.net.ssl.trustStore=" + keyStore.toAbsolutePath(),
                "-Djavax.net.ssl.trustStorePassword=password",
                "-cp", classpath,
                "org.soprasteria.avans.lockercloud.syncserver.ServerBootstrap",
                "--server.port=0"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        int port = -1;
        long end = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < end && (line = reader.readLine()) != null) {
            if (line.startsWith("PORT:")) {
                port = Integer.parseInt(line.substring(5));
                break;
            }
        }
        assertTrue(port > 0);

        new ProcessBuilder("kill", "-SIGINT", String.valueOf(process.pid())).start().waitFor(5, TimeUnit.SECONDS);
        boolean exited = process.waitFor(10, TimeUnit.SECONDS);
        assertTrue(exited);

        try (var socket = new java.net.Socket("localhost", port)) {
            fail("Should not connect after SIGINT");
        } catch (Exception expected) {
            // ignore
        }
    }
}
