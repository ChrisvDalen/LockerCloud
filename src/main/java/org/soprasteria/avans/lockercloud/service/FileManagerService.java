package org.soprasteria.avans.lockercloud.service;


import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileChunk;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileManagerService {

    private final Path storageLocation = Paths.get("filestorage");

    public FileManagerService() {
        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create storage directory", e);
        }
    }

    public void saveFile(MultipartFile file) {
        try {
            Path targetLocation = storageLocation.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Error saving file " + file.getOriginalFilename(), e);
        }
    }

    public byte[] getFile(String fileName) {
        try {
            Path filePath = storageLocation.resolve(fileName);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Error reading file " + fileName, e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = storageLocation.resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Error deleting file " + fileName, e);
        }
    }

    public List<String> listFiles() {
        try (Stream<Path> stream = Files.list(storageLocation)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileStorageException("Error listing files", e);
        }
    }

    public void syncFiles() {
        // Implement synchronization logic:
        // Compare file checksums, upload missing files, download updated files, etc.
        // For now, you can leave this as a stub or log that sync was requested.
        System.out.println("Sync operation requested.");
    }

    // Future methods for file chunking could be added here.
    // For example:
    // public void saveFileChunk(FileChunk chunk) { ... }
    // public void assembleChunks(String fileName) { ... }
}

