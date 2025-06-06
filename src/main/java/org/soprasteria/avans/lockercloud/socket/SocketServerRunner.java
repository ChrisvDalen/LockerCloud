package org.soprasteria.avans.lockercloud.socket;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Starts the SSLFileServer on application startup.
 */
@Component
public class SocketServerRunner implements CommandLineRunner {

    @Value("${socket.port:9000}")
    private int port;

    private final FileManagerService fileManagerService;

    @Autowired
    public SocketServerRunner(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @Override
    public void run(String... args) {
        SSLFileServer server = new SSLFileServer(port, fileManagerService);
        Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();
    }
}
