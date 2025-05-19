package org.soprasteria.avans.lockercloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileMetadata;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class FileManagerServiceTest {

    @TempDir
    Path storageDir;

    @TempDir
    Path clientDir;

    private FileManagerService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new FileManagerService();
        // override private final storageLocation
        Field storageField = FileManagerService.class.getDeclaredField("storageLocation");
        storageField.setAccessible(true);
        storageField.set(service, storageDir);
        // override private final clientLocalLocation
        Field clientField = FileManagerService.class.getDeclaredField("clientLocalLocation");
        clientField.setAccessible(true);
        clientField.set(service, clientDir);
        // ensure dirs exist
        Files.createDirectories(storageDir);
        Files.createDirectories(clientDir);
    }

    @Test
    void saveFile_smallFile_shouldWriteToStorage() throws Exception {
        byte[] content = "hello".getBytes();
        MultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain", content);

        service.saveFile(file);

        Path written = storageDir.resolve("hello.txt");
        assertTrue(Files.exists(written), "File should be created");
        assertArrayEquals(content, Files.readAllBytes(written));
    }

    @Test
    void saveFile_ioError_shouldThrowFileStorageException() throws Exception {
        MultipartFile file = spy(new MockMultipartFile("file", "error.txt", "text/plain", new byte[0]));
        when(file.getInputStream()).thenThrow(new java.io.IOException("disk full"));

        FileStorageException ex = assertThrows(FileStorageException.class, () -> service.saveFile(file));
        assertTrue(ex.getMessage().contains("Error saving file error.txt"));
    }

    @Test
    void saveFile_largeFile_shouldWriteChunk() throws Exception {
        byte[] data = "chunk".getBytes();
        MultipartFile raw = new MockMultipartFile("file", "big.bin", "application/octet-stream", data);
        MultipartFile file = spy(raw);
        // threshold is 100MB
        long threshold = 100L * 1024 * 1024;
        when(file.getSize()).thenReturn(threshold + 1);

        service.saveFile(file);

        Path chunk = storageDir.resolve("big.bin.part1");
        assertTrue(Files.exists(chunk), "Chunkbestand moet zijn geschreven");
        assertArrayEquals(data, Files.readAllBytes(chunk));
    }

    @Test
    void getFile_existingFile_shouldReturnBytes() throws Exception {
        byte[] data = "data".getBytes();
        Files.write(storageDir.resolve("f.txt"), data);

        byte[] result = service.getFile("f.txt");
        assertArrayEquals(data, result);
    }

    @Test
    void getFile_chunks_shouldConcatenateInOrder() throws Exception {
        byte[] a = "A".getBytes(), b = "BB".getBytes();
        Files.write(storageDir.resolve("file.part2"), b);
        Files.write(storageDir.resolve("file.part1"), a);

        byte[] result = service.getFile("file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(a);
        baos.write(b);
        assertArrayEquals(baos.toByteArray(), result);
    }

    @Test
    void getFile_notFound_shouldThrowFileStorageException() {
        FileStorageException ex = assertThrows(FileStorageException.class, () -> service.getFile("nofile"));
        assertTrue(ex.getMessage().contains("File not found: nofile"));
    }

    @Test
    void deleteFile_shouldRemoveFileAndChunks() throws Exception {
        Path f = storageDir.resolve("d.txt");
        Path chunk = storageDir.resolve("d.txt.part1");
        Files.write(f, "x".getBytes());
        Files.write(chunk, "y".getBytes());

        service.deleteFile("d.txt");

        assertFalse(Files.exists(f));
        assertFalse(Files.exists(chunk));
    }

    @Test
    void listFiles_shouldReturnOnlyRegularFilesExcludingChunks() throws Exception {
        Files.write(storageDir.resolve("a.txt"), new byte[]{});
        Files.write(storageDir.resolve("b.bin"), new byte[]{});
        Files.write(storageDir.resolve("a.txt.part1"), new byte[]{});

        List<String> names = service.listFiles();
        assertTrue(names.contains("a.txt"));
        assertTrue(names.contains("b.bin"));
        assertFalse(names.stream().anyMatch(n -> n.endsWith(".part1")));
    }

    @Test
    void syncFiles_shouldDetectUploadsDownloadsAndConflicts() throws Exception {
        // prepare server files
        String s1 = "one.txt", s2 = "two.txt";
        byte[] c1 = "hello".getBytes(), c2 = "orld".getBytes();
        Files.write(storageDir.resolve(s1), c1);
        Files.write(storageDir.resolve(s2), c2);

        // client metadata
        String checksum1 = md5(c1);
        long now = System.currentTimeMillis();
        FileMetadata m1 = new FileMetadata(s1, checksum1, c1.length, LocalDateTime.now(), now);
        FileMetadata m2 = new FileMetadata(s2, "deadbeef", c2.length, LocalDateTime.now(), now);
        FileMetadata m3 = new FileMetadata("three.txt", "abc", 0, LocalDateTime.now(), now);
        List<FileMetadata> client = Arrays.asList(m1, m2, m3);

        SyncResult res = service.syncFiles(client);

        assertEquals(List.of("three.txt"), res.getFilesToUpload());
        assertTrue(res.getFilesToDownload().isEmpty(), "No new files to download");
        assertEquals(List.of("two.txt"), res.getConflictFiles());
    }

    private String md5(byte[] data) throws Exception {
        var md = MessageDigest.getInstance("MD5");
        md.update(data);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
