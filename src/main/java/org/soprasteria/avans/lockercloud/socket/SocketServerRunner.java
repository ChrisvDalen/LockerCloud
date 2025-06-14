package org.soprasteria.avans.lockercloud.socket;

import jakarta.annotation.PreDestroy;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts {@link SocketFileServer} when the Spring Boot application runs so
 * clients can communicate using the socket-based protocol.
 */
@Component
public class SocketServerRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SocketServerRunner.class);

    private final FileManagerService fileManager;
    private final int port;
    private SocketFileServer server;
    private Thread serverThread;

    public SocketServerRunner(FileManagerService fileManager,
                              @Value("${file.socket.port:9090}") int port) {
        this.fileManager = fileManager;
        this.port = port;
    }

    @Override
    public void run(String... args) {
        server = new SocketFileServer(port, fileManager);
        serverThread = new Thread(server);
        serverThread.setDaemon(true);
        serverThread.start();
        log.info("SocketFileServer launched on port {}", port);
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (server != null) {
            log.info("Stopping SocketFileServer");
            server.stop();
            serverThread.join(1000);
        }
    }
}
