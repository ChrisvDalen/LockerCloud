package org.example.client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("Host:");
        String host = sc.nextLine();
        while (true) {
            System.out.print("cmd> ");
            String cmd = sc.nextLine();
            if (cmd.startsWith("upload")) {
                Path p = Path.of(cmd.split(" ")[1]);
                upload(host, p);
            } else if (cmd.equals("list")) {
                listFiles(host);
            } else if (cmd.startsWith("download")) {
                download(host, cmd.split(" ")[1]);
            } else if (cmd.startsWith("delete")) {
                delete(host, cmd.split(" ")[1]);
            } else if (cmd.equals("exit")) {
                break;
            }
        }
    }

    private static void upload(String host, Path file) throws Exception {
        byte[] data = Files.readAllBytes(file);
        String header = "POST /upload\nContent-Length: " + data.length + "\nContent-Disposition: " + file.getFileName() + "\n\n";
        try (Socket socket = new Socket(host, 8080)) {
            OutputStream out = socket.getOutputStream();
            out.write(header.getBytes());
            out.write(data);
            out.flush();
            System.out.println(new String(socket.getInputStream().readNBytes(64)));
        }
    }

    private static void listFiles(String host) throws Exception {
        String header = "POST /listFiles\nContent-Length: 0\n\n";
        try (Socket socket = new Socket(host, 8080)) {
            OutputStream out = socket.getOutputStream();
            out.write(header.getBytes());
            out.flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    private static void download(String host, String file) throws Exception {
        String header = "GET /download?file=" + file + "\nHost: " + host + "\n\n";
        try (Socket socket = new Socket(host, 8080)) {
            OutputStream out = socket.getOutputStream();
            out.write(header.getBytes());
            out.flush();
            InputStream in = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String start = br.readLine();
            System.out.println(start);
            String lenLine = br.readLine();
            int len = Integer.parseInt(lenLine.split(":")[1].trim());
            br.readLine();
            byte[] data = in.readNBytes(len);
            Files.write(Path.of(file), data);
            System.out.println("downloaded " + len + " bytes");
        }
    }

    private static void delete(String host, String file) throws Exception {
        String header = "DELETE /delete?file=" + file + "\nContent-Length: 0\n\n";
        try (Socket socket = new Socket(host, 8080)) {
            OutputStream out = socket.getOutputStream();
            out.write(header.getBytes());
            out.flush();
            System.out.println(new String(socket.getInputStream().readNBytes(64)));
        }
    }
}
