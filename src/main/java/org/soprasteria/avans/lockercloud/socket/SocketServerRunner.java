package org.soprasteria.avans.lockercloud.socket;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.soprasteria.avans.lockercloud.socket.WebSocketFileServer;
import org.soprasteria.avans.lockercloud.socket.StaticFileServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Starts the WebSocketFileServer on application startup.
 */
@Component
public class SocketServerRunner implements CommandLineRunner {

    @Value("${http.port:8443}")
    private int httpPort;

    @Value("${socket.port:8444}")
    private int socketPort;

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    private final FileManagerService fileManagerService;

    @Autowired
    public SocketServerRunner(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @Override
    public void run(String... args) {
        String path = keyStorePath;
        if (path.startsWith("classpath:")) {
            path = path.substring("classpath:".length());
        } else if (path.startsWith("file:")) {
            path = path.substring("file:".length());
        }
        try {
            StaticFileServer httpServer = new StaticFileServer(httpPort, path, keyStorePassword);
            httpServer.start();
            WebSocketFileServer server = new WebSocketFileServer(socketPort, fileManagerService,
                    path, keyStorePassword);
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start socket servers", e);
        }
    }
}
