package org.soprasteria.avans.lockercloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.soprasteria.avans.lockercloud.helper.FaultyMultipartFile;
import org.soprasteria.avans.lockercloud.helper.LargeInputStream;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class NetworkFaultIntegrationTest {

    @TempDir
    Path storageDir;
    @TempDir
    Path clientDir;

    private FileManagerService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new FileManagerService();
        Field storageField = FileManagerService.class.getDeclaredField("storageLocation");
        storageField.setAccessible(true);
        storageField.set(service, storageDir);
        Field clientField = FileManagerService.class.getDeclaredField("clientLocalLocation");
        clientField.setAccessible(true);
        clientField.set(service, clientDir);
        Files.createDirectories(storageDir);
        Files.createDirectories(clientDir);
    }

    @Test
    void smallFileUpload_retriesOnDrop() throws Exception {
        byte[] data = "network".getBytes();
        Supplier<InputStream> supplier = () -> new java.io.ByteArrayInputStream(data);
        FaultyMultipartFile file = new FaultyMultipartFile(
                "file", "net.txt", "text/plain",
                supplier,
                data.length,
                1,
                3); // drop after 3 bytes on first attempt

        service.saveFileWithRetry(file, null);

        assertEquals(2, file.getAttemptCount(), "Should retry once");
        Path target = storageDir.resolve("net.txt");
        assertTrue(Files.exists(target));
        assertArrayEquals(data, Files.readAllBytes(target));
    }

    @Test
    void largeFileUpload_retriesAndNoLeftoverChunks() throws Exception {
        long chunkSize = 1024; // use small chunk to keep test fast
        Field chunkSizeField = FileManagerService.class.getDeclaredField("CHUNK_SIZE");
        chunkSizeField.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(chunkSizeField, chunkSizeField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        chunkSizeField.set(null, chunkSize);

        long threshold = 2048; // force large file path
        Field thresholdField = FileManagerService.class.getDeclaredField("CHUNK_THRESHOLD");
        thresholdField.setAccessible(true);
        modifiers.setInt(thresholdField, thresholdField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        thresholdField.set(null, threshold);

        long dataSize = chunkSize * 3 + 10;
        Supplier<InputStream> supplier = () -> new LargeInputStream(dataSize, (byte) 'A');
        FaultyMultipartFile file = new FaultyMultipartFile(
                "file", "big.bin", "application/octet-stream",
                supplier,
                dataSize,
                2,
                chunkSize / 2); // fail twice

        service.saveFileWithRetry(file, null);

        assertEquals(3, file.getAttemptCount());
        Path target = storageDir.resolve("big.bin");
        assertTrue(Files.exists(target));
        assertEquals(dataSize, Files.size(target));
        // ensure no leftover chunk files
        long partCount = Files.list(storageDir)
                .filter(p -> p.getFileName().toString().startsWith("big.bin.part"))
                .count();
        assertEquals(0, partCount, "Chunks should be cleaned up");
    }

    @Test
    void concurrentUploads_underFaultsComplete() throws Exception {
        byte[] data = "abcde".getBytes();
        ExecutorService exec = Executors.newFixedThreadPool(3);
        Callable<Void> task = () -> {
            String name = Thread.currentThread().getName() + ".txt";
            Supplier<InputStream> sup = () -> new java.io.ByteArrayInputStream(data);
            FaultyMultipartFile f = new FaultyMultipartFile("f", name, "text/plain", sup, data.length, 1, 2);
            service.saveFileWithRetry(f, null);
            return null;
        };

        Future<Void> f1 = exec.submit(task);
        Future<Void> f2 = exec.submit(task);
        Future<Void> f3 = exec.submit(task);
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        f3.get(5, TimeUnit.SECONDS);
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));

        long files = Files.list(storageDir).filter(Files::isRegularFile).count();
        assertEquals(3, files);
    }
}
