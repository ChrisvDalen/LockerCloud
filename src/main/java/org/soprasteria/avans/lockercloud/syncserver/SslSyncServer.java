package org.soprasteria.avans.lockercloud.syncserver;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple SSL server that accepts connections and echoes back the first line sent by the client.
 * Provides start/stop/isRunning API and performs graceful shutdown of resources.
 */
public class SslSyncServer {
    private static final Logger logger = LoggerFactory.getLogger(SslSyncServer.class);

    private final int port;
    private final ExecutorService executorService;
    private SSLServerSocket serverSocket;
    private Thread acceptThread;
    private Thread shutdownHook;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SslSyncServer(int port, ExecutorService executorService) {
        this.port = port;
        this.executorService = executorService;
    }

    public synchronized void start() throws Exception {
        if (running.get()) {
            return;
        }
        SSLServerSocketFactory factory = createSSLServerSocketFactory();
        serverSocket = (SSLServerSocket) factory.createServerSocket(port);
        acceptThread = new Thread(this::acceptLoop, "accept-thread");
        shutdownHook = new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }, "shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        running.set(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                executorService.submit(new ClientHandler((SSLSocket) socket));
            } catch (IOException e) {
                if (running.get()) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignore) {
                // JVM is already shutting down
            }
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (acceptThread != null) {
            try {
                acceptThread.join(TimeUnit.SECONDS.toMillis(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        if (serverSocket != null) {
            return serverSocket.getLocalPort();
        }
        return -1;
    }

    private SSLServerSocketFactory createSSLServerSocketFactory() throws Exception {
        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
        String password = System.getProperty("javax.net.ssl.keyStorePassword");
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = new FileInputStream(keyStorePath)) {
            keyStore.load(in, password.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, null);
        return context.getServerSocketFactory();
    }

    private static class ClientHandler implements Runnable {
        private final SSLSocket socket;

        ClientHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    writer.write("ACK:" + line + "\n");
                    writer.flush();
                }
            } catch (IOException e) {
                // log and continue
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
