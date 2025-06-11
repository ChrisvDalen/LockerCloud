package org.soprasteria.avans.lockercloud.syncserver;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

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
        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 1000L * 60 * 60);
        Date notAfter = new Date(now + 365L * 24L * 60L * 60L * 1000L);
        X500Name dn = new X500Name("CN=Test");

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dn,
                BigInteger.valueOf(now),
                notBefore,
                notAfter,
                dn,
                keyPair.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = certBuilder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        ks.setKeyEntry("alias", keyPair.getPrivate(), password.toCharArray(), new Certificate[]{cert});

        Path file = Files.createTempFile("test-keystore", ".jks");
        try (OutputStream out = Files.newOutputStream(file)) {
            ks.store(out, password.toCharArray());
        }
        return file;
    }
}
