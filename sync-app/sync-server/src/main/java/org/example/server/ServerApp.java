package org.example.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private final Path storage = Path.of("storage");
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws Exception {
        new ServerApp().start();
    }

    public void start() throws Exception {
        Files.createDirectories(storage);
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server listening on 8080");
            while (true) {
                Socket client = serverSocket.accept();
                pool.submit(() -> handle(client));
            }
        }
    }

    private void handle(Socket client) {
        try (InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream()) {
            Protocol.Request req = Protocol.parseRequest(in);
            switch (req.method) {
                case "GET" -> handleGet(req, out);
                case "POST" -> handlePost(req, out);
                case "DELETE" -> handleDelete(req, out);
                default -> writeSimple(out, 400, "Bad Request");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGet(Protocol.Request req, OutputStream out) throws IOException {
        if (req.path.startsWith("/download")) {
            String file = req.path.substring(req.path.indexOf('=') + 1);
            Path p = storage.resolve(Path.of(file).getFileName());
            if (Files.exists(p)) {
                byte[] data = Files.readAllBytes(p);
                writeResponse(out, 200, "OK", data);
            } else {
                writeSimple(out, 404, "Not Found");
            }
        } else {
            writeSimple(out, 400, "Bad Request");
        }
    }

    private void handlePost(Protocol.Request req, OutputStream out) throws IOException {
        if (req.path.equals("/upload")) {
            String name = req.headers.getOrDefault("Content-Disposition", "upload.bin");
            Path p = storage.resolve(Path.of(name).getFileName());
            Files.write(p, req.body);
            writeSimple(out, 200, "Uploaded");
        } else if (req.path.equals("/listFiles")) {
            List<String> files = Files.list(storage)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
            byte[] data = ("[" + String.join(",",
                    files.stream().map(s -> "\"" + s + "\"").toList()) + "]")
                    .getBytes();
            writeResponse(out, 200, "OK", data);
        } else if (req.path.equals("/sync")) {
            // simple stub
            writeSimple(out, 200, "Synced");
        } else {
            writeSimple(out, 400, "Bad Request");
        }
    }

    private void handleDelete(Protocol.Request req, OutputStream out) throws IOException {
        if (req.path.startsWith("/delete")) {
            String file = req.path.substring(req.path.indexOf('=') + 1);
            Path p = storage.resolve(Path.of(file).getFileName());
            if (Files.exists(p)) {
                Files.delete(p);
                writeSimple(out, 200, "Deleted");
            } else {
                writeSimple(out, 404, "Not Found");
            }
        } else {
            writeSimple(out, 400, "Bad Request");
        }
    }

    private void writeSimple(OutputStream out, int code, String text) throws IOException {
        writeResponse(out, code, text, new byte[0]);
    }

    private void writeResponse(OutputStream out, int code, String text, byte[] body) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        bw.write("HTTP/1.1 " + code + " " + text + "\r\n");
        bw.write("Content-Length: " + body.length + "\r\n");
        bw.write("Access-Control-Allow-Origin: *\r\n");
        bw.write("\r\n");
        bw.flush();
        out.write(body);
        out.flush();
    }
}
