package org.soprasteria.avans.lockercloud.service;

import org.soprasteria.avans.lockercloud.exception.FileStorageException;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Minimal file management service for the socket based server.
 * Stores files in a configured directory and supports chunked uploads.
 */
public class FileManagerService {
    private static final long CHUNK_THRESHOLD = 4L * 1024 * 1024 * 1024; // 4 GB
    private static final long CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path storageLocation;

    public FileManagerService(Path storageLocation) {
        this.storageLocation = storageLocation;
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create storage directory", e);
        }
    }

    /**
     * Save a file from an InputStream. Large files are written in chunks.
     */
    public void saveStream(String fileName, InputStream input) {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(input, "input");
        String normalized = Paths.get(fileName).getFileName().toString();
        Path target = storageLocation.resolve(normalized);
        try {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Error saving file " + normalized, e);
        }
    }

    /**
     * Retrieve a file or assemble it from chunk parts.
     */
    public byte[] getFile(String fileName) {
        String normalized = Paths.get(fileName).getFileName().toString();
        Path filePath = storageLocation.resolve(normalized);
        if (Files.exists(filePath)) {
            try {
                return Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new FileStorageException("Error reading file " + normalized, e);
            }
        }
        // attempt to reassemble from chunks
        try (Stream<Path> stream = Files.list(storageLocation)) {
            List<Path> chunks = stream
                    .filter(p -> p.getFileName().toString().startsWith(normalized + ".part"))
                    .sorted(Comparator.comparingInt(p -> extractChunkIndex(p.getFileName().toString(), normalized)))
                    .collect(Collectors.toList());
            if (chunks.isEmpty()) {
                throw new FileStorageException("File not found: " + normalized);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (Path chunk : chunks) {
                out.write(Files.readAllBytes(chunk));
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new FileStorageException("Error reading file chunks for " + normalized, e);
        }
    }

    /** Delete a file and any chunk parts. */
    public void deleteFile(String fileName) {
        String normalized = Paths.get(fileName).getFileName().toString();
        try {
            Files.deleteIfExists(storageLocation.resolve(normalized));
            deleteChunks(normalized);
        } catch (IOException e) {
            throw new FileStorageException("Error deleting file " + normalized, e);
        }
    }

    /** List all stored files excluding chunk parts. */
    public List<String> listFiles() {
        try (Stream<Path> stream = Files.list(storageLocation)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> !n.contains(".part"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public String calculateChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    private void deleteChunks(String baseName) throws IOException {
        try (Stream<Path> stream = Files.list(storageLocation)) {
            stream.filter(p -> p.getFileName().toString().startsWith(baseName + ".part"))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) { }
                    });
        }
    }

    private int extractChunkIndex(String chunkName, String base) {
        String prefix = base + ".part";
        if (chunkName.startsWith(prefix)) {
            try {
                return Integer.parseInt(chunkName.substring(prefix.length()));
            } catch (NumberFormatException ignored) { }
        }
        return Integer.MAX_VALUE;
    }
}
