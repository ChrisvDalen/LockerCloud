package org.soprasteria.avans.lockercloud.socket;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

/**
 * Minimal HTTPS server that serves the web interface.
 */
public class StaticFileServer {
    private final int port;
    private final String keyStorePath;
    private final String password;
    private HttpsServer server;

    public StaticFileServer(int port, String keyStorePath, String password) {
        this.port = port;
        this.keyStorePath = keyStorePath;
        this.password = password;
    }

    public void start() throws Exception {
        SSLContext ctx = createContext();
        server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ctx));
        server.createContext("/", new IndexHandler());
        server.start();
    }

    private SSLContext createContext() throws Exception {
        char[] pass = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(keyStorePath)) {
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

    private static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String page = loadIndex();
            byte[] bytes = page.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String loadIndex() throws IOException {
            try (InputStream is = IndexHandler.class.getClassLoader().getResourceAsStream("index.html")) {
                if (is == null) {
                    return "<html><body><h1>LockerCloud</h1></body></html>";
                }
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
    }
}
