package org.soprasteria.avans.lockercloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ConcurrencyFileManagerServiceTest {

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
    void concurrentSaveAndRead_ShouldNotBlockAndFinalContentConsistent() throws Exception {
        String fileName = "concurrent.txt";
        int threads = 2;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        Callable<Void> task = () -> {
            barrier.await();
            String payload = Thread.currentThread().getName();
            MultipartFile file = spy(new MockMultipartFile(
                    "file", fileName, "text/plain", payload.getBytes(StandardCharsets.UTF_8)));
            when(file.getSize()).thenReturn((long) payload.getBytes().length);
            try {
                service.saveFile(file);
            } catch (FileStorageException ignore) {
                // allow race-related failures
            }
            try {
                byte[] data = service.getFile(fileName);
                assertNotNull(data, "Reader must get non-null data");
            } catch (FileStorageException ignore) {
                //filler code
            }
            return null;
        };

        Future<Void> f1 = exec.submit(task);
        Future<Void> f2 = exec.submit(task);

        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate");

        Path target = storageDir.resolve(fileName);
        assertTrue(Files.exists(target), "File must exist after concurrent writes");

        String content = Files.readString(target, StandardCharsets.UTF_8).trim();
        // Should match thread-naming pattern and not be corrupted
        assertTrue(content.matches("pool-\\d+-thread-\\d+"),
                "Final file content should match a thread name pattern, was: " + content);
    }
}
