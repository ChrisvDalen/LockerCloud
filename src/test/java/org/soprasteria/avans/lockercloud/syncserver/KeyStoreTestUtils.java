package org.soprasteria.avans.lockercloud.syncserver;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
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
        Path file = Files.createTempFile("test-keystore", ".jks");

        ProcessBuilder pb = new ProcessBuilder(
                "keytool", "-genkeypair",
                "-alias", "alias",
                "-keyalg", "RSA",
                "-keystore", file.toString(),
                "-storepass", password,
                "-keypass", password,
                "-dname", "CN=Test",
                "-storetype", "JKS",
                "-validity", "365");
        Process p = pb.start();
        if (!p.waitFor(5, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("keytool timed out");
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("keytool failed with exit code " + p.exitValue());
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream in = Files.newInputStream(file)) {
            ks.load(in, password.toCharArray());
        }

        return file;
    }
}
