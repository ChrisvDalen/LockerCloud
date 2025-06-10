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

    @Value("${socket.port:8443}")
    private int port;

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
        SSLFileServer server = new SSLFileServer(port, fileManagerService,
                path, keyStorePassword);
        Thread t = new Thread(server);
        t.setDaemon(true);
        t.start();
    }
}
