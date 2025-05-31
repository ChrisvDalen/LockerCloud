package org.soprasteria.avans.lockercloud.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileManagerService {

    // Voor grote bestanden (demonstratie: 100 MB threshold, in productie 4GB)
    private static final Logger logger = LoggerFactory.getLogger(FileManagerService.class);
    private static final long CHUNK_THRESHOLD = 100L * 1024 * 1024; // 100 MB
    private static final long CHUNK_SIZE = 10L * 1024 * 1024; // 10 MB

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
         if (file.isEmpty()) {
            throw new FileStorageException("Cannot save an empty file.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileStorageException("File name cannot be null or empty.");
        }
        // Normalize filename to prevent directory traversal issues
        String normalizedFilename = Paths.get(originalFilename).getFileName().toString();


        if (file.getSize() > CHUNK_THRESHOLD) {
            saveLargeFile(file);
        } else {
            try {
                Path targetLocation = storageLocation.resolve(normalizedFilename);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new FileStorageException("Error saving file " + normalizedFilename, e);
            }
        }
    }

    @Retryable(retryFor = { IOException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void saveFileWithRetry(MultipartFile file) {
        saveFile(file);
    }

    @Recover
    public void recoverSaveFile(IOException e, MultipartFile file) { // Corrected signature
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            deleteFileChunks(fileName); // Also delete the potentially incomplete main file if not chunked
            try {
                Files.deleteIfExists(storageLocation.resolve(fileName));
            } catch (IOException ex) {
                logger.error("Failed to delete main file during recovery: {} " + fileName);
            }
        }
        throw new FileStorageException("Failed to upload file '" + fileName + "' after retries.", e);
    }

    private void saveLargeFile(MultipartFile file) {
        String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[(int) CHUNK_SIZE];
            int bytesRead;
            int chunkIndex = 1;
            List<Path> writtenChunks = new ArrayList<>();

            // 1) Schrijf alle chunks
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                String chunkName = originalFileName + ".part" + chunkIndex++;
                Path chunkPath = storageLocation.resolve(chunkName);
                try (OutputStream os = Files.newOutputStream(chunkPath,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    os.write(buffer, 0, bytesRead);
                }
                writtenChunks.add(chunkPath);
            }

            // 2) Assembleer ze meteen in één bestand
            Path finalPath = storageLocation.resolve(originalFileName);
            try (OutputStream finalOs = Files.newOutputStream(finalPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                // sorteren op index zodat de volgorde klopt
                writtenChunks.stream()
                        .sorted(Comparator.comparing(p -> {
                            String s = p.getFileName().toString()
                                    .replace(originalFileName + ".part", "");
                            return Integer.parseInt(s);
                        }))
                        .forEach(chunkPath -> {
                            try {
                                Files.copy(chunkPath, finalOs);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }

            // 3) (Optioneel) verwijder de chunk-bestanden
            for (Path chunk : writtenChunks) {
                Files.deleteIfExists(chunk);
            }

        } catch (IOException e) {
            // bestaande cleanup
            deleteFileChunks(originalFileName);
            throw new FileStorageException("Error saving large file " + originalFileName, e);
        }
    }


    private void deleteFileChunks(String originalFileName) {
        if (originalFileName == null) return;
        String normalizedOriginalFileName = Paths.get(originalFileName).getFileName().toString();
        try (Stream<Path> stream = Files.list(storageLocation)) {
            stream.filter(path -> path.getFileName().toString().startsWith(normalizedOriginalFileName + ".part"))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            logger.error("Failed to delete chunk {}: {}", path.getFileName().toString(), ex.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.error("Error listing directory for deleting chunks of {}: {}", normalizedOriginalFileName, e.getMessage());
        }
    }

    public byte[] getFileFallback(String fileName, Throwable t) {
        logger.error("CircuitBreaker tripped on getFile: {} " + t.getMessage());
        return new byte[0]; // of null, of een specifieke error-indicator
    }

    @CircuitBreaker(name = "fileService", fallbackMethod = "getFileFallback")
    @Retryable(retryFor = { IOException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public byte[] getFile(String fileName) {
        String normalizedFileName = Paths.get(fileName).getFileName().toString(); // Normalize
        Path filePath = storageLocation.resolve(normalizedFileName);
        if (Files.exists(filePath)) {
            try {
                return Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new FileStorageException("Error reading file " + normalizedFileName, e);
            }
        } else {
            // Attempt to reassemble from chunks if main file not found
            try (Stream<Path> stream = Files.list(storageLocation)) {
                List<Path> chunks = stream
                        .filter(path -> path.getFileName().toString().startsWith(normalizedFileName + ".part"))
                        .sorted(Comparator.comparingInt(p -> extractChunkIndex(p.getFileName().toString(), normalizedFileName)))
                        .collect(Collectors.toList());
                if (chunks.isEmpty()) {
                    throw new FileStorageException("File not found (and no chunks): " + normalizedFileName);
                }
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (Path chunk : chunks) {
                    byte[] chunkData = Files.readAllBytes(chunk);
                    outputStream.write(chunkData);
                }
                return outputStream.toByteArray();
            } catch (IOException e) {
                throw new FileStorageException("Error reading file chunks for " + normalizedFileName, e);
            }
        }
    }

    @Recover
    public byte[] recoverGetFile(IOException e, String fileName) {
        // cleanup if needed, log, then throw or return an error sentinel
        deleteFileChunks(fileName);
        throw new FileStorageException("Failed to download '" + fileName + "' after retries", e);
    }

    private int extractChunkIndex(String chunkFileName, String originalFileName) {
        String prefix = originalFileName + ".part";
        if (chunkFileName.startsWith(prefix)) {
            try {
                return Integer.parseInt(chunkFileName.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // Malformed chunk name
                logger.error("Malformed chunk index for: {}" + chunkFileName);
                return Integer.MAX_VALUE; // Sorts malformed to the end
            }
        }
        return Integer.MAX_VALUE; // Not a valid chunk name for this original file
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        String normalizedFileName = Paths.get(fileName).getFileName().toString(); // Normalize
        try {
            Path filePath = storageLocation.resolve(normalizedFileName);
            Files.deleteIfExists(filePath);
            deleteFileChunks(normalizedFileName); // Delete any associated chunks

            // Also delete from clientLocalLocation if it exists there to keep them in sync
            Path clientSyncFilePath = clientLocalLocation.resolve(normalizedFileName);
            Files.deleteIfExists(clientSyncFilePath);
            logger.info("Deleted '{}' from master and client sync locations.", normalizedFileName);
        } catch (IOException e) {
            throw new FileStorageException("Error deleting file " + normalizedFileName, e);
        }
    }

    public List<String> listFiles() {
        try (Stream<Path> stream = Files.list(storageLocation)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !name.contains(".part")) // Exclude chunk files from list
                    .sorted() // Sort for consistent order
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing files from master storage: {} " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public SyncResult syncFiles(List<FileMetadata> clientProvidedMetadataList) {
        Map<String, FileMetadata> clientMap = clientProvidedMetadataList.stream()
            .collect(Collectors.toMap(FileMetadata::getFileName, fm -> fm, (fm1, fm2) -> fm1)); // Handle duplicates if any

        Map<String, FileMetadata> serverMasterMap = getDirectoryMetadata(this.storageLocation); // Use helper

        Set<String> allFileNames = new HashSet<>();
        allFileNames.addAll(clientMap.keySet());
        allFileNames.addAll(serverMasterMap.keySet());

        List<String> toUpload = new ArrayList<>();    // Files client has that server doesn't, or client is newer
        List<String> toDownload = new ArrayList<>();  // Files server has that client doesn't, or server is newer
        List<String> conflicts = new ArrayList<>();

        for (String name : allFileNames) {
            FileMetadata cMeta = clientMap.get(name);
            FileMetadata sMeta = serverMasterMap.get(name);

            if (sMeta == null) {
                if (cMeta != null) toUpload.add(name);
                continue;
            }
            if (cMeta == null) {
                toDownload.add(name);
                continue;
            }
            if (cMeta.getChecksum() != null && sMeta.getChecksum() != null &&
                cMeta.getChecksum().equals(sMeta.getChecksum())) {
                continue;
            }
            if (cMeta.getLastModified() > sMeta.getLastModified()) {
                toUpload.add(name);
            } else if (sMeta.getLastModified() > cMeta.getLastModified()) {
                toDownload.add(name);
            } else {
                conflicts.add(name);
            }
        }
        return new SyncResult(toUpload, toDownload, conflicts);
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
                            logger.error("Error reading client file " + path.getFileName().toString());
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
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new FileStorageException("File name is null or empty for transactional save.");
        }
        String normalizedFileName = Paths.get(originalFileName).getFileName().toString(); // Normalize

        Path tempPath = storageLocation.resolve(normalizedFileName + ".tmp");
        Path finalPath = storageLocation.resolve(normalizedFileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
            String actualChecksum = calculateChecksum(tempPath);

            if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
                Files.deleteIfExists(tempPath);
                throw new FileStorageException("Checksum mismatch for file " + normalizedFileName);
            }
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                logger.error("Failed to delete temp file {} during transactional save error handling.", tempPath);
            }
            throw new FileStorageException("Failed transactional save for " + normalizedFileName, e);
        }
    }

    public SyncResult analyzeLocalClientDifferences() {
        List<FileMetadata> clientLocalFilesMetadata = new ArrayList<>();
        try (Stream<Path> stream = Files.list(clientLocalLocation)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> !path.getFileName().toString().contains(".part"))
                  .forEach(path -> {
                try {
                    String fileName = path.getFileName().toString();
                    long fileSize = Files.size(path);
                    String checksum = calculateChecksum(path);
                    LocalDateTime fileTime = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
                    long lastMod = Files.getLastModifiedTime(path).toMillis();
                    
                    FileMetadata meta = new FileMetadata(fileName, checksum, fileSize, fileTime, lastMod);
                    clientLocalFilesMetadata.add(meta);
                } catch (IOException e) {
                    logger.error("Error generating metadata for client file '{}': {}", path.getFileName().toString(), e.getMessage());
                }
            });
        } catch (IOException e) {
            throw new FileStorageException("Error reading client local sync directory for analysis", e);
        }
        // Compare this clientLocalFilesMetadata with the server's master storage
        return syncFiles(clientLocalFilesMetadata);
    }

    @Async
    public CompletableFuture<SyncResult> performServerSideLocalSyncAsync() { // Changed to call the new method
        SyncResult result = performServerSideLocalSync();
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Generates a map of FileMetadata for all regular files in a given directory.
     * Skips .part files.
     * @param directoryPath The path to the directory to scan.
     * @return A map where keys are file names and values are their FileMetadata.
     */
    private Map<String, FileMetadata> getDirectoryMetadata(Path directoryPath) {
        Map<String, FileMetadata> metadataMap = new HashMap<>();
        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            logger.error("Metadata Scan: Directory does not exist or is not a directory: {}", directoryPath);
            return metadataMap;
        }
        try (Stream<Path> stream = Files.list(directoryPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> !path.getFileName().toString().contains(".part")) // Skip chunk files
                  .forEach(filePath -> {
                try {
                    String name = filePath.getFileName().toString();
                    long lastModMillis = Files.getLastModifiedTime(filePath).toMillis();
                    String checksum = calculateChecksum(filePath);
                    long fileSize = Files.size(filePath);
                    LocalDateTime fileTimestamp = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(filePath).toInstant(), ZoneId.systemDefault()
                    );

                    // Using the full constructor for FileMetadata
                    FileMetadata meta = new FileMetadata(name, checksum, fileSize, fileTimestamp, lastModMillis);
                    metadataMap.put(name, meta);
                } catch (IOException e) {
                    logger.error("Error generating metadata for file '{}' in directory '{}': {}",
                            filePath.getFileName(), directoryPath, e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.error("Error listing directory '{}': {}", directoryPath, e.getMessage());
        }
        return metadataMap;
    }

    /**
     * Performs a server-side synchronization between the primary storageLocation (master)
     * and the clientLocalLocation (local mirror).
     * It copies files as needed to make clientLocalLocation a reflection of storageLocation,
     * and vice-versa, based on file differences.
     *
     * @return SyncResult summarizing the operations performed.
     */
    public SyncResult performServerSideLocalSync() {
        logger.info("Performing server-side local sync...");
        Map<String, FileMetadata> clientSyncFilesMetadata = getDirectoryMetadata(this.clientLocalLocation);
        Map<String, FileMetadata> serverMasterFilesMetadata = getDirectoryMetadata(this.storageLocation);

        Set<String> allFileNames = new HashSet<>();
        allFileNames.addAll(clientSyncFilesMetadata.keySet());
        allFileNames.addAll(serverMasterFilesMetadata.keySet());

        List<String> filesToCopyToClientLocal = new ArrayList<>();
        List<String> filesToCopyToServerMaster = new ArrayList<>();
        List<String> conflictFiles = new ArrayList<>();

        for (String name : allFileNames) {
            FileMetadata clientMeta = clientSyncFilesMetadata.get(name);
            FileMetadata serverMeta = serverMasterFilesMetadata.get(name);

            if (serverMeta == null) {
                if (clientMeta != null) {
                    filesToCopyToServerMaster.add(name);
                }
                continue;
            }
            if (clientMeta == null) {
                filesToCopyToClientLocal.add(name);
                continue;
            }

            // File exists in both locations, compare them
            // Ensure checksums are not null before comparing
            if (clientMeta.getChecksum() != null && serverMeta.getChecksum() != null &&
                clientMeta.getChecksum().equals(serverMeta.getChecksum())) {
                continue;
            }

            if (clientMeta.getLastModified() > serverMeta.getLastModified()) {
                filesToCopyToServerMaster.add(name);
            } else if (serverMeta.getLastModified() > clientMeta.getLastModified()) {
                filesToCopyToClientLocal.add(name);
            } else {
                conflictFiles.add(name);
            }
        }

        // --- Perform actual file copy operations ---
        List<String> successfullyCopiedToClient = new ArrayList<>();
        for (String fileName : filesToCopyToClientLocal) {
            try {
                Path sourcePath = storageLocation.resolve(fileName);
                Path destinationPath = clientLocalLocation.resolve(fileName);
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                successfullyCopiedToClient.add(fileName);
                logger.info("SYNC: Copied '{}' from MASTER_STORAGE to CLIENT_SYNC_DIR.", fileName);
            } catch (IOException e) {
                logger.error("SYNC: FAILED to copy '{}' to CLIENT_SYNC_DIR: {}", fileName, e.getMessage());
                conflictFiles.add(fileName + " (copy to clientSync failed)"); // Add to conflicts if copy fails
            }
        }

        List<String> successfullyCopiedToServer = new ArrayList<>();
        for (String fileName : filesToCopyToServerMaster) {
            try {
                Path sourcePath = clientLocalLocation.resolve(fileName);
                Path destinationPath = storageLocation.resolve(fileName);
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                successfullyCopiedToServer.add(fileName);
                logger.info("SYNC: Copied '{}' from CLIENT_SYNC_DIR to MASTER_STORAGE.", fileName);
            } catch (IOException e) {
                logger.error("SYNC: FAILED to copy '{}' to MASTER_STORAGE: {}", fileName, e.getMessage());
                conflictFiles.add(fileName + " (copy to serverStorage failed)"); // Add to conflicts
            }
        }

        loggerlogger.info("Server-side local sync completed. Copied to client: {}, Copied to server: {}, Conflicts: {}",
                successfullyCopiedToClient.size(), successfullyCopiedToServer.size(), conflictFiles.size());

        return new SyncResult(successfullyCopiedToServer, successfullyCopiedToClient, conflictFiles);
    }
}
