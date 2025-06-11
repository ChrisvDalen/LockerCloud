package org.soprasteria.avans.lockercloud.syncserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Utility class for generating a temporary keystore for SSL tests without
 *  relying on internal JDK APIs. */
public final class KeyStoreTestUtils {
    private KeyStoreTestUtils() {}

    /**
     * Creates a temporary JKS keystore with a single self-signed certificate
     * using the {@code keytool} command line utility.
     *
     * @param password the password for the keystore
     * @return the path to the generated keystore
     */
    public static Path createTempKeyStore(String password) throws Exception {
        Path file = Files.createTempFile("test-keystore", ".jks");
        ProcessBuilder pb = new ProcessBuilder(
                "keytool",
                "-genkeypair",
                "-alias", "alias",
                "-dname", "CN=Test",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "365",
                "-storetype", "JKS",
                "-keystore", file.toString(),
                "-storepass", password,
                "-keypass", password,
                "-noprompt"
        );
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();
        if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IOException("keytool execution failed with code " + process.exitValue());
        }
        return file;
    }
}
