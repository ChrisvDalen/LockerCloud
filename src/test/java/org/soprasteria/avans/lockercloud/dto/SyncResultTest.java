package org.soprasteria.avans.lockercloud.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncResultTest {

    @Test
    void defaultConstructor_fieldsShouldBeNull() {
        SyncResult result = new SyncResult();
        assertNull(result.getFilesToUpload(), "filesToUpload should be null by default");
        assertNull(result.getFilesToDownload(), "filesToDownload should be null by default");
        assertNull(result.getConflictFiles(), "conflictFiles should be null by default");
    }

    @Test
    void setAndGetFilesToUpload_shouldReturnSameListInstance() {
        SyncResult result = new SyncResult();
        List<String> uploadList = new ArrayList<>(Arrays.asList("a.txt", "b.txt"));
        result.setFilesToUpload(uploadList);

        List<String> returned = result.getFilesToUpload();
        assertSame(uploadList, returned, "getFilesToUpload should return the exact list instance set");
        assertEquals(2, returned.size());
        assertIterableEquals(Arrays.asList("a.txt", "b.txt"), returned);
    }

    @Test
    void setAndGetFilesToDownload_shouldReturnSameListInstance() {
        SyncResult result = new SyncResult();
        List<String> downloadList = new ArrayList<>(Collections.singletonList("c.txt"));
        result.setFilesToDownload(downloadList);

        List<String> returned = result.getFilesToDownload();
        assertSame(downloadList, returned, "getFilesToDownload should return the exact list instance set");
        assertEquals(1, returned.size());
        assertEquals("c.txt", returned.get(0));
    }

    @Test
    void setAndGetConflictFiles_shouldReturnSameListInstance() {
        SyncResult result = new SyncResult();
        List<String> conflictList = Arrays.asList("x.txt", "y.txt", "z.txt");
        result.setConflictFiles(conflictList);

        List<String> returned = result.getConflictFiles();
        assertSame(conflictList, returned, "getConflictFiles should return the exact list instance set");
        assertIterableEquals(Arrays.asList("x.txt", "y.txt", "z.txt"), returned);
    }

    @Test
    void settingNullLists_shouldAllowNullAndGetterShouldReturnNull() {
        SyncResult result = new SyncResult();
        result.setFilesToUpload(null);
        result.setFilesToDownload(null);
        result.setConflictFiles(null);

        assertNull(result.getFilesToUpload(), "filesToUpload should be null after setting null");
        assertNull(result.getFilesToDownload(), "filesToDownload should be null after setting null");
        assertNull(result.getConflictFiles(), "conflictFiles should be null after setting null");
    }

    @Test
    void settingEmptyLists_shouldReturnEmptyLists() {
        SyncResult result = new SyncResult();
        List<String> empty = Collections.emptyList();
        result.setFilesToUpload(empty);
        result.setFilesToDownload(empty);
        result.setConflictFiles(empty);

        assertNotNull(result.getFilesToUpload());
        assertTrue(result.getFilesToUpload().isEmpty(), "filesToUpload should be empty");
        assertNotNull(result.getFilesToDownload());
        assertTrue(result.getFilesToDownload().isEmpty(), "filesToDownload should be empty");
        assertNotNull(result.getConflictFiles());
        assertTrue(result.getConflictFiles().isEmpty(), "conflictFiles should be empty");
    }
}
