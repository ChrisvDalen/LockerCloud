package org.soprasteria.avans.lockercloud.socketapp;

import org.soprasteria.avans.lockercloud.service.FileManagerService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private final Config config;
    private final FileManagerService service;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SocketServer(Config config) {
        this.config = config;
        this.service = new FileManagerService(config.getStoragePath());
    }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(config.getPort())) {
            while (true) {
                Socket s = server.accept();
                executor.submit(new FileTransferHandler(s, service, config.getAesKey()));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String file = args.length > 0 ? args[0] : "config.properties";
        Config cfg = new Config(file);
        new SocketServer(cfg).start();
    }
}
