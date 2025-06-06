package org.soprasteria.avans.lockercloud.syncserver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bootstrap class to start the {@link SslSyncServer} from command line.
 */
public class ServerBootstrap {
    public static void main(String[] args) throws Exception {
        int port = 8443;
        for (String arg : args) {
            if (arg.startsWith("--server.port=")) {
                port = Integer.parseInt(arg.substring("--server.port=".length()));
            }
        }
        port = Integer.parseInt(System.getProperty("server.port", String.valueOf(port)));
        ExecutorService executor = Executors.newFixedThreadPool(4);
        SslSyncServer server = new SslSyncServer(port, executor);
        server.start();
        System.out.println("PORT:" + server.getPort());
        while (server.isRunning()) {
            Thread.sleep(1000);
        }
    }
}
