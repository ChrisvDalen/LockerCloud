package org.soprasteria.avans.lockercloud.controller;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.soprasteria.avans.lockercloud.service.FileManagerService;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static org.mockito.Mockito.*;

class FileControllerTest {

    @Mock
    private FileManagerService fileManagerService;

    @InjectMocks
    private FileController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void index_returnsIndexView() {
        assertEquals("index", controller.index());
    }

    @Test
    void uploadFile_success() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        String view = controller.uploadFile(file, attrs);

        assertEquals("redirect:/", view);
        assertTrue(attrs.getFlashAttributes().containsKey("uploadSuccess"));
        assertEquals("Bestand test.txt succesvol geüpload!", attrs.getFlashAttributes().get("uploadSuccess"));
        verify(fileManagerService).saveFileWithRetry(file);
    }

    @Test
    void uploadFile_error() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "bad.txt", "text/plain", "data".getBytes());
        doThrow(new RuntimeException("oops")).when(fileManagerService).saveFileWithRetry(file);
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        String view = controller.uploadFile(file, attrs);

        assertEquals("redirect:/", view);
        assertTrue(attrs.getFlashAttributes().containsKey("uploadError"));
        assertEquals("Fout bij uploaden: oops", attrs.getFlashAttributes().get("uploadError"));
        verify(fileManagerService).saveFileWithRetry(file);
    }

    @Test
    void downloadFile_success() throws Exception {
        byte[] data = {1,2,3};
        when(fileManagerService.getFile("f.bin")).thenReturn(data);

        ResponseEntity<byte[]> resp = controller.downloadFile("f.bin");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("attachment; filename=\"f.bin\"", resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, resp.getHeaders().getContentType());
        assertArrayEquals(data, resp.getBody());
    }

    @Test
    void downloadFile_error() {
        // throw unchecked to satisfy Mockito
        when(fileManagerService.getFile("x")).thenThrow(new RuntimeException("nf"));

        ResponseEntity<byte[]> resp = controller.downloadFile("x");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    void downloadAllFiles_noFiles() throws Exception {
        when(fileManagerService.listFiles()).thenReturn(Collections.emptyList());

        ResponseEntity<byte[]> resp = controller.downloadAllFiles();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("attachment; filename=\"all-files.zip\"", resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(resp.getBody()))) {
            assertNull(zis.getNextEntry());
        }
    }

    @Test
    void downloadAllFiles_multipleFiles() throws Exception {
        when(fileManagerService.listFiles()).thenReturn(Arrays.asList("a.txt","b.txt"));
        when(fileManagerService.getFile("a.txt")).thenReturn(new byte[]{10});
        when(fileManagerService.getFile("b.txt")).thenReturn(new byte[]{20});

        ResponseEntity<byte[]> resp = controller.downloadAllFiles();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("attachment; filename=\"all-files.zip\"", resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));

        Map<String, byte[]> entries = readZip(resp.getBody());
        assertArrayEquals(new byte[]{10}, entries.get("a.txt"));
        assertArrayEquals(new byte[]{20}, entries.get("b.txt"));
    }

    @Test
    void downloadAllFiles_skipNull() throws Exception {
        when(fileManagerService.listFiles()).thenReturn(Arrays.asList("ok","bad","ok2"));
        when(fileManagerService.getFile("ok")).thenReturn(new byte[]{1});
        when(fileManagerService.getFile("bad")).thenReturn(null);
        when(fileManagerService.getFile("ok2")).thenReturn(new byte[]{2});

        ResponseEntity<byte[]> resp = controller.downloadAllFiles();
        Map<String, byte[]> entries = readZip(resp.getBody());

        assertTrue(entries.containsKey("ok"));
        assertTrue(entries.containsKey("ok2"));
        assertFalse(entries.containsKey("bad"));
    }

    @Test
    void downloadAllFiles_listThrows() {
        when(fileManagerService.listFiles()).thenThrow(new RuntimeException("nope"));

        ResponseEntity<byte[]> resp = controller.downloadAllFiles();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    void downloadAllFiles_ioException() {
        when(fileManagerService.listFiles()).thenReturn(Collections.singletonList("x"));
        // again throw unchecked
        when(fileManagerService.getFile("x")).thenThrow(new RuntimeException("ioerr"));

        ResponseEntity<byte[]> resp = controller.downloadAllFiles();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertNull(resp.getBody());
    }


    private Map<String, byte[]> readZip(byte[] zipBytes) throws IOException {
        Map<String, byte[]> map = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                zis.transferTo(bos);
                map.put(entry.getName(), bos.toByteArray());
                zis.closeEntry();
            }
        }
        return map;
    }

    @Test
    void deleteFile_success() throws Exception {
        doNothing().when(fileManagerService).deleteFile("f");

        ResponseEntity<String> resp = controller.deleteFile("f");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("File deleted successfully", resp.getBody());
    }

    @Test
    void deleteFile_error() throws Exception {
        doThrow(new RuntimeException("fail del")).when(fileManagerService).deleteFile("f");

        ResponseEntity<String> resp = controller.deleteFile("f");

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Error deleting file: fail del", resp.getBody());
    }

    @Test
    void listFiles_success() throws Exception {
        when(fileManagerService.listFiles()).thenReturn(Arrays.asList("a","b"));

        ResponseEntity<?> resp = controller.listFiles();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(Arrays.asList("a","b"), resp.getBody());
    }

    @Test
    void listFiles_error() throws Exception {
        when(fileManagerService.listFiles()).thenThrow(new RuntimeException("list err"));

        ResponseEntity<?> resp = controller.listFiles();

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Error listing files: list err", resp.getBody());
    }

    @Test
    void syncFiles_success() throws Exception {
        List<FileMetadata> client = List.of(new FileMetadata());
        SyncResult result = new SyncResult();
        when(fileManagerService.syncFiles(client)).thenReturn(result);

        ResponseEntity<?> resp = controller.syncFiles(client);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(result, resp.getBody());
    }

    @Test
    void syncFiles_error() throws Exception {
        List<FileMetadata> client = List.of();
        when(fileManagerService.syncFiles(client)).thenThrow(new RuntimeException("sync fail"));

        ResponseEntity<?> resp = controller.syncFiles(client);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Error syncing files: sync fail", resp.getBody());
    }
}
