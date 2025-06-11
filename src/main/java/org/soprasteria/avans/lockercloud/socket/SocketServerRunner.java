package org.soprasteria.avans.lockercloud.socket;

import jakarta.annotation.PreDestroy;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Starts {@link SocketFileServer} when the Spring Boot application runs so
 * clients can communicate using the socket-based protocol.
 */
@Component
public class SocketServerRunner implements CommandLineRunner {

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
    }

    @PreDestroy
    public void stop() throws InterruptedException {
        if (server != null) {
            server.stop();
            serverThread.join(1000);
        }
    }
}
