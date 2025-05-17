package org.soprasteria.avans.lockercloud.service;

import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileManagerService {

    // Voor grote bestanden (demonstratie: 100 MB threshold, in productie 4GB)
    private static final long CHUNK_THRESHOLD = 100 * 1024 * 1024; // 100 MB
    private static final long CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path storageLocation = Paths.get("filestorage");
    // Simuleer de lokale client map (bijvoorbeeld een synchronisatie map op de client)
    private final Path clientLocalLocation = Paths.get("clientSync");

    public FileManagerService() {
        try {
            Files.createDirectories(storageLocation);
            Files.createDirectories(clientLocalLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create storage directories", e);
        }
    }

    // Bestaande methoden (saveFile, getFile, deleteFile, listFiles) blijven grotendeels hetzelfde

    public void saveFile(MultipartFile file) {
        if (file.getSize() > CHUNK_THRESHOLD) {
            saveLargeFile(file);
        } else {
            try {
                Path targetLocation = storageLocation.resolve(file.getOriginalFilename());
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FileStorageException("Error saving file " + file.getOriginalFilename(), e);
            }
        }
    }

    @Retryable(value = { IOException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void saveFileWithRetry(MultipartFile file) {
        saveFile(file);
    }

    @Retryable
    public void recoverSaveFile(MultipartFile file, IOException e) {
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            deleteFileChunks(fileName);
        }
        throw new FileStorageException("Failed to upload file after retries: " + fileName, e);
    }

    private void saveLargeFile(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new FileStorageException("File name is null");
        }
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[(int) CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String chunkFileName = originalFileName + ".part" + chunkIndex;
                Path chunkPath = storageLocation.resolve(chunkFileName);
                try (OutputStream os = Files.newOutputStream(chunkPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    os.write(buffer, 0, bytesRead);
                }
                chunkIndex++;
            }
        } catch (IOException e) {
            deleteFileChunks(originalFileName);
            throw new FileStorageException("Error saving large file " + originalFileName, e);
        }
    }

    private void deleteFileChunks(String originalFileName) {
        try (Stream<Path> stream = Files.list(storageLocation)) {
            stream.filter(path -> path.getFileName().toString().startsWith(originalFileName + ".part"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            System.err.println("Failed to delete chunk " + path.getFileName().toString());
                        }
                    });
        } catch (IOException e) {
            // Log indien nodig
        }
    }

    public byte[] getFile(String fileName) {
        Path filePath = storageLocation.resolve(fileName);
        if (Files.exists(filePath)) {
            try {
                return Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new FileStorageException("Error reading file " + fileName, e);
            }
        } else {
            try (Stream<Path> stream = Files.list(storageLocation)) {
                List<Path> chunks = stream
                        .filter(path -> path.getFileName().toString().startsWith(fileName + ".part"))
                        .sorted(Comparator.comparingInt(p -> extractChunkIndex(p.getFileName().toString(), fileName)))
                        .collect(Collectors.toList());
                if (chunks.isEmpty()) {
                    throw new FileStorageException("File not found: " + fileName);
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (Path chunk : chunks) {
                    byte[] chunkData = Files.readAllBytes(chunk);
                    outputStream.write(chunkData);
                }
                return outputStream.toByteArray();
            } catch (IOException e) {
                throw new FileStorageException("Error reading file chunks for " + fileName, e);
            }
        }
    }

    private int extractChunkIndex(String chunkFileName, String originalFileName) {
        String prefix = originalFileName + ".part";
        try {
            return Integer.parseInt(chunkFileName.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = storageLocation.resolve(fileName);
            Files.deleteIfExists(filePath);
            deleteFileChunks(fileName);
        } catch (IOException e) {
            throw new FileStorageException("Error deleting file " + fileName, e);
        }
    }

    public List<String> listFiles() {
        try (Stream<Path> stream = Files.list(storageLocation)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.contains(".part"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileStorageException("Error listing files", e);
        }
    }

    // Bestaande syncFiles-methode voor conflictbeheer
    public SyncResult syncFiles(List<FileMetadata> clientFiles) {
        SyncResult result = new SyncResult();
        List<String> filesToUpload = new ArrayList<>();
        List<String> filesToDownload = new ArrayList<>();
        List<String> conflictFiles = new ArrayList<>();

        Map<String, FileMetadata> clientMap = clientFiles.stream()
                .collect(Collectors.toMap(FileMetadata::getFileName, fm -> fm));

        List<String> serverFiles = listFiles();

        for (FileMetadata clientFile : clientFiles) {
            Path serverFilePath = storageLocation.resolve(clientFile.getFileName());
            if (Files.exists(serverFilePath)) {
                try {
                    String serverChecksum = calculateChecksum(serverFilePath);
                    if (!serverChecksum.equals(clientFile.getChecksum())) {
                        conflictFiles.add(clientFile.getFileName());
                    }
                } catch (IOException e) {
                    throw new FileStorageException("Error computing checksum for file " + clientFile.getFileName(), e);
                }
            } else {
                filesToUpload.add(clientFile.getFileName());
            }
        }

        for (String serverFileName : serverFiles) {
            if (!clientMap.containsKey(serverFileName)) {
                filesToDownload.add(serverFileName);
            }
        }

        result.setFilesToUpload(filesToUpload);
        result.setFilesToDownload(filesToDownload);
        result.setConflictFiles(conflictFiles);
        return result;
    }

    // Helper om MD5-checksum te berekenen
    private String calculateChecksum(Path filePath) throws IOException {
        try (InputStream fis = Files.newInputStream(filePath)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not found", e);
        }
    }

    /**
     * Automatische synchronisatie: Lees de "clientSync" map (simuleert de lokale client-bestanden),
     * bereken metadata voor elk bestand en roep de bestaande syncFiles-methode aan.
     */
    public SyncResult syncLocalClientFiles() {
        // Zorg dat de clientSync map bestaat (wordt al aangemaakt in de constructor)
        List<FileMetadata> clientFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(clientLocalLocation)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            FileMetadata meta = new FileMetadata();
                            meta.setFileName(path.getFileName().toString());
                            meta.setFileSize(Files.size(path));
                            meta.setChecksum(calculateChecksum(path));
                            meta.setUploadDate(LocalDateTime.now());
                            clientFiles.add(meta);
                        } catch (IOException e) {
                            System.err.println("Error reading client file " + path.getFileName().toString());
                        }
                    });
        } catch (IOException e) {
            throw new FileStorageException("Error reading client local sync directory", e);
        }
        return syncFiles(clientFiles);
    }

    @Async
    public CompletableFuture<SyncResult> syncLocalClientFilesAsync() {
        SyncResult result = syncLocalClientFiles();
        return CompletableFuture.completedFuture(result);
    }

    public void saveFileTransactional(MultipartFile file, String expectedChecksum) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new FileStorageException("File name is null");
        }

        Path tempPath = storageLocation.resolve(originalFileName + ".tmp");
        Path finalPath = storageLocation.resolve(originalFileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
            String actualChecksum = calculateChecksum(tempPath);

            if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
                Files.deleteIfExists(tempPath);
                throw new FileStorageException("Checksum mismatch for file " + originalFileName);
            }

            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignore) {}
            throw new FileStorageException("Failed transactional save for " + originalFileName, e);
        }
    }

}
