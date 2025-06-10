package org.soprasteria.avans.lockercloud.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;
import org.soprasteria.avans.lockercloud.service.FileManagerService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Simple WebSocket server for file operations. Commands are JSON objects:
 * {"command":"list"}
 * {"command":"upload","file":"name","data":"<base64>"}
 * {"command":"download","file":"name"}
 * {"command":"delete","file":"name"}
 */
public class WebSocketFileServer extends WebSocketServer {
    private final FileManagerService fileService;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebSocketFileServer(int port, FileManagerService fileService,
                               String keyStorePath, String password) throws Exception {
        super(new InetSocketAddress(port));
        this.fileService = fileService;
        SSLContext ctx = createContext(keyStorePath, password);
        setWebSocketFactory(new DefaultSSLWebSocketServerFactory(ctx));
    }

    private SSLContext createContext(String path, String pwd) throws Exception {
        char[] pass = pwd.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(path)) {
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

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // no-op
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // no-op
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> req = mapper.readValue(message, Map.class);
            String cmd = (String) req.get("command");
            if (cmd == null) {
                conn.send("{\"error\":\"missing command\"}");
                return;
            }
            switch (cmd) {
                case "list" -> {
                    List<String> files = fileService.listFiles();
                    conn.send(mapper.writeValueAsString(Map.of("files", files)));
                }
                case "delete" -> {
                    String name = (String) req.get("file");
                    fileService.deleteFile(name);
                    conn.send("{\"status\":\"ok\"}");
                }
                case "download" -> {
                    String name = (String) req.get("file");
                    byte[] data = fileService.getFile(name);
                    String b64 = Base64.getEncoder().encodeToString(data);
                    conn.send(mapper.writeValueAsString(Map.of("file", name, "data", b64)));
                }
                case "upload" -> {
                    String name = (String) req.get("file");
                    String data = (String) req.get("data");
                    byte[] bytes = Base64.getDecoder().decode(data);
                    try (InputStream in = new ByteArrayInputStream(bytes)) {
                        fileService.saveStream(name, in);
                    }
                    conn.send("{\"status\":\"ok\"}");
                }
                default -> conn.send("{\"error\":\"unknown command\"}");
            }
        } catch (Exception e) {
            conn.send("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // ignore
    }

    @Override
    public void onStart() {
        // no-op
    }
}
