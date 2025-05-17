package org.soprasteria.avans.lockercloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.dockerjava.zerodep.shaded.org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileManagerServiceTest {

    private FileManagerService fileManagerService;
    private final Path storageLocation = Paths.get("filestorage");

    @BeforeEach
    void setUp() throws IOException {
        fileManagerService = new FileManagerService();
        Files.createDirectories(storageLocation);
    }

    @Test
    void testSaveFileTransactionalSuccess() throws IOException {
        byte[] content = "Hello CloudLocker".getBytes();
        String checksum = md5Hex(content);
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content);

        assertDoesNotThrow(() -> fileManagerService.saveFileTransactional(file, checksum));

        Path savedFile = storageLocation.resolve("test.txt");
        assertTrue(Files.exists(savedFile));
        assertEquals(checksum, md5Hex(Files.readAllBytes(savedFile)));
    }

    @Test
    void testSaveFileTransactionalChecksumMismatch() throws IOException {
        byte[] content = "This is wrong".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "mismatch.txt", "text/plain", content);

        FileStorageException ex = assertThrows(FileStorageException.class, () ->
                fileManagerService.saveFileTransactional(file, "00000000000000000000000000000000"));

        assertTrue(ex.getMessage().contains("Checksum mismatch"));
        assertFalse(Files.exists(storageLocation.resolve("mismatch.txt")));
        assertFalse(Files.exists(storageLocation.resolve("mismatch.txt.tmp")));
    }

    @Test
    void saveFile() {
    }

    @Test
    void saveFileWithRetry() {
    }

    @Test
    void recoverSaveFile() {
    }

    @Test
    void getFile() {
    }

    @Test
    void deleteFile() {
    }

    @Test
    void listFiles() {
    }

    @Test
    void syncFiles() {
    }

    @Test
    void syncLocalClientFiles() {
    }

    @Test
    void syncLocalClientFilesAsync() {
    }

    @Test
    void saveFileTransactional() {
    }
}