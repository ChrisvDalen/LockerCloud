package org.soprasteria.avans.lockercloud.syncserver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Utility class for generating an in-memory keystore for SSL tests. */
public final class KeyStoreTestUtils {
    private KeyStoreTestUtils() {}

    /**
     * Creates a temporary JKS keystore containing a single self-signed certificate.
     *
     * @param password the password for the keystore
     * @return path to the created keystore file
     */
    public static Path createTempKeyStore(String password) throws Exception {
        Path file = Files.createTempDirectory("test-keystore").resolve("keystore.jks");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        Process process = new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias", "alias",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "365",
                "-dname", "CN=Test",
                "-storetype", "JKS",
                "-keystore", file.toString(),
                "-storepass", password,
                "-keypass", password,
                "-noprompt")
                .redirectErrorStream(true)
                .start();

        if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException(
                    "Could not create test keystore: " + new String(process.getInputStream().readAllBytes()));
        }
        return file;
    }
}
