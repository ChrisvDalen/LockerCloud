package org.soprasteria.avans.lockercloud.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simple CLI WebSocket client to interact with the WebSocketFileServer.
 */
public class WebSocketClientApp {
    private static final String HOST = System.getProperty("server.host", "localhost");
    private static final int PORT = Integer.getInteger("socket.port", 8444);
    private static final String KEYSTORE = System.getProperty("client.keystore", "keystore.p12");
    private static final String PASSWORD = System.getProperty("client.keystorePassword", "YourPasswordHere");
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { usage(); return; }
        String cmd = args[0];
        switch (cmd) {
            case "list" -> send(Map.of("command", "list"));
            case "delete" -> {
                if (args.length != 2) { usage(); return; }
                send(Map.of("command", "delete", "file", args[1]));
            }
            case "download" -> {
                if (args.length != 3) { usage(); return; }
                Map<String, Object> resp = send(Map.of("command", "download", "file", args[1]));
                if (resp != null && resp.containsKey("data")) {
                    byte[] data = Base64.getDecoder().decode((String) resp.get("data"));
                    java.nio.file.Files.write(java.nio.file.Path.of(args[2]), data);
                }
            }
            case "upload" -> {
                if (args.length != 2) { usage(); return; }
                byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[1]));
                String b64 = Base64.getEncoder().encodeToString(bytes);
                send(Map.of("command", "upload", "file", java.nio.file.Path.of(args[1]).getFileName().toString(), "data", b64));
            }
            default -> usage();
        }
    }

    private static Map<String, Object> send(Map<String, Object> request) throws Exception {
        URI uri = new URI("wss://" + HOST + ":" + PORT);
        CountDownLatch latch = new CountDownLatch(1);
        final String[] msg = new String[1];
        WebSocketClient client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                try {
                    send(mapper.writeValueAsString(request));
                } catch (Exception e) {
                    latch.countDown();
                }
            }
            @Override
            public void onMessage(String message) {
                msg[0] = message;
                latch.countDown();
            }
            @Override public void onClose(int code, String reason, boolean remote) { latch.countDown(); }
            @Override public void onError(Exception ex) { latch.countDown(); }
        };
        client.setSocketFactory(createContext().getSocketFactory());
        client.connectBlocking();
        latch.await(10, TimeUnit.SECONDS);
        client.close();
        if (msg[0] != null) {
            System.out.println(msg[0]);
            return mapper.readValue(msg[0], Map.class);
        }
        return null;
    }

    private static SSLContext createContext() throws Exception {
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
        return ctx;
    }

    private static void usage() {
        System.out.println("Usage: WebSocketClientApp [list|upload <file>|download <name> <dest>|delete <name>]");
    }
}
