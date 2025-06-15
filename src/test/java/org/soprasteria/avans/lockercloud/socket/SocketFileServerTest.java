package org.soprasteria.avans.lockercloud.socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.service.FileManagerService;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SocketFileServerTest {

    private SocketFileServer server;
    private Thread serverThread;
    private FileManagerService fileManager;

    @BeforeEach
    void setup() {
        fileManager = mock(FileManagerService.class);
        server = new SocketFileServer(9090, fileManager);
        serverThread = new Thread(server);
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        server.stop();
        serverThread.join(200);
    }

    @Test
    void uploadAndDownload_flow() throws Exception {
        byte[] data = "hello".getBytes();
        when(fileManager.saveFileStream(eq("test.txt"), any(InputStream.class), eq((long) data.length), eq("abc"))).thenReturn("abc");
        when(fileManager.getFile("test.txt")).thenReturn(data);
        when(fileManager.getFileChecksum("test.txt")).thenReturn("abc");

        try (SocketFileClient client = new SocketFileClient("localhost", 9090)) {
            String status = client.upload("test.txt", data);
            assertTrue(status.contains("200"));

            SocketFileClient.DownloadResult dl = client.download("test.txt");
            assertArrayEquals(data, dl.data);
            assertEquals("abc", dl.checksum);
        }
    }

    @Test
    void listFiles_shouldReturnNames() throws Exception {
        when(fileManager.listFiles()).thenReturn(java.util.List.of("a.txt", "b.bin"));

        try (SocketFileClient client = new SocketFileClient("localhost", 9090)) {
            String names = client.listFiles();
            assertTrue(names.contains("a.txt"));
            assertTrue(names.contains("b.bin"));
        }

        verify(fileManager).listFiles();
    }

    @Test
    void delete_shouldInvokeService() throws Exception {
        doNothing().when(fileManager).deleteFile("gone.txt");

        try (SocketFileClient client = new SocketFileClient("localhost", 9090)) {
            String status = client.delete("gone.txt");
            assertTrue(status.contains("200"));
        }

        verify(fileManager).deleteFile("gone.txt");
    }

    @Test
    void sync_shouldReturnStatus() throws Exception {
        when(fileManager.performServerSideLocalSync()).thenReturn(new SyncResult(java.util.List.of(), java.util.List.of(), java.util.List.of()));

        try (SocketFileClient client = new SocketFileClient("localhost", 9090)) {
            String status = client.sync();
            assertTrue(status.contains("200"));
        }

        verify(fileManager).performServerSideLocalSync();
    }
}
