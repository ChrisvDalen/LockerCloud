package com.example.cloudsync;

import org.glassfish.tyrus.server.Server;

public class ServerMain {
    public static void main(String[] args) {
        Server server = new Server("localhost", 8080, "/", null, SyncEndpoint.class);
        try {
            server.start();
            System.out.println("Server started. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
