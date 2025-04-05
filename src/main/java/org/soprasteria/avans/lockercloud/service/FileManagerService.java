package org.soprasteria.avans.lockercloud.service;


import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileChunk;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


    @Service
    public class FileManagerService {

        // Voor demonstratie; in productie vervang je dit met: 4L * 1024 * 1024 * 1024
        private static final long CHUNK_THRESHOLD = 100 * 1024 * 1024; // 100 MB
        private static final long CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB

        private final Path storageLocation = Paths.get("filestorage");

        public FileManagerService() {
            try {
                Files.createDirectories(storageLocation);
            } catch (IOException e) {
                throw new FileStorageException("Could not create storage directory", e);
            }
        }

        // Bestaande methode: als het bestand groter is dan de threshold, gebruik chunking
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

        // Retry-logica voor file upload (werkt zowel voor kleine als grote bestanden)
        @Retryable(value = { IOException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
        public void saveFileWithRetry(MultipartFile file) {
            saveFile(file);
        }

        // Als alle pogingen falen, verwijder eventuele gedeeltelijke uploads
        // Deze recover-methode wordt aangeroepen door Spring Retry.
        @Retryable
        public void recoverSaveFile(MultipartFile file, IOException e) {
            String fileName = file.getOriginalFilename();
            if (fileName != null) {
                deleteFileChunks(fileName);
            }
            throw new FileStorageException("Failed to upload file after retries: " + fileName, e);
        }

        // Methode om grote bestanden in chunks op te slaan
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

        // Verwijder alle chunks voor een bepaald bestand (rollback)
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

        // Ophalen van een bestand: als het originele bestand niet bestaat, controleer dan op chunk-bestanden en voeg deze samen
        public byte[] getFile(String fileName) {
            Path filePath = storageLocation.resolve(fileName);
            if (Files.exists(filePath)) {
                try {
                    return Files.readAllBytes(filePath);
                } catch (IOException e) {
                    throw new FileStorageException("Error reading file " + fileName, e);
                }
            } else {
                // Zoek naar chunk-bestanden
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
                // Verwijder eerst het hoofd-bestand
                Path filePath = storageLocation.resolve(fileName);
                Files.deleteIfExists(filePath);
                // Verwijder ook eventuele chunk-bestanden
                deleteFileChunks(fileName);
            } catch (IOException e) {
                throw new FileStorageException("Error deleting file " + fileName, e);
            }
        }

        public List<String> listFiles() {
            try (Stream<Path> stream = Files.list(storageLocation)) {
                // Haal alleen de hoofd-bestanden op (geen chunks)
                return stream.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> !name.contains(".part"))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new FileStorageException("Error listing files", e);
            }
        }

        // De bestaande syncFiles-methode (voor conflictbeheer staat dit later in het protocol)
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
    }
