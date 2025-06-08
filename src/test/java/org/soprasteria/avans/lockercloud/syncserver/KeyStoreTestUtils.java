package org.soprasteria.avans.lockercloud.syncserver;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

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

        CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA256withRSA");
        gen.generate(2048);
        X509Certificate cert = gen.getSelfCertificate(new X500Name("CN=Test"), 365 * 24L * 60L * 60L);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("alias", gen.getPrivateKey(), password.toCharArray(), new Certificate[]{cert});

        try (OutputStream out = Files.newOutputStream(file)) {
            ks.store(out, password.toCharArray());
        }
        return file;
    }
}
