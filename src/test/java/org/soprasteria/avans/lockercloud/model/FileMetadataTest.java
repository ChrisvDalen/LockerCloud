package org.soprasteria.avans.lockercloud.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FileMetadataTest {

    @Test
    void defaultConstructor_ShouldInitializeFieldsToDefaults() {
        FileMetadata metadata = new FileMetadata();

        assertNull(metadata.getFileName(), "fileName should be null by default");
        assertNull(metadata.getChecksum(), "checksum should be null by default");
        assertEquals(0L, metadata.getFileSize(), "fileSize should be 0 by default");
        assertNull(metadata.getUploadDate(), "uploadDate should be null by default");
    }

    @Test
    void allArgsConstructor_ShouldSetAllFields() {
        String name = "document.pdf";
        String checksum = "abc123";
        long size = 1024L;
        LocalDateTime now = LocalDateTime.of(2025, 5, 17, 12, 30);

        FileMetadata metadata = new FileMetadata(name, checksum, size, now);

        assertEquals(name, metadata.getFileName(), "fileName should match constructor argument");
        assertEquals(checksum, metadata.getChecksum(), "checksum should match constructor argument");
        assertEquals(size, metadata.getFileSize(), "fileSize should match constructor argument");
        assertSame(now, metadata.getUploadDate(), "uploadDate should match constructor argument instance");
    }

    @Test
    void settersAndGetters_ShouldUpdateAndReturnFields() {
        FileMetadata metadata = new FileMetadata();

        String name = "image.png";
        String checksum = "def456";
        long size = 2048L;
        LocalDateTime time = LocalDateTime.now();

        metadata.setFileName(name);
        metadata.setChecksum(checksum);
        metadata.setFileSize(size);
        metadata.setUploadDate(time);

        assertEquals(name, metadata.getFileName());
        assertEquals(checksum, metadata.getChecksum());
        assertEquals(size, metadata.getFileSize());
        assertSame(time, metadata.getUploadDate());
    }

    @Test
    void setters_ShouldAllowNullAndEdgeValues() {
        FileMetadata metadata = new FileMetadata();

        // Allow nulls
        metadata.setFileName(null);
        metadata.setChecksum(null);
        metadata.setUploadDate(null);
        assertNull(metadata.getFileName(), "fileName should accept null");
        assertNull(metadata.getChecksum(), "checksum should accept null");
        assertNull(metadata.getUploadDate(), "uploadDate should accept null");

        // Edge values for fileSize
        metadata.setFileSize(-1L);
        assertEquals(-1L, metadata.getFileSize(), "fileSize should accept negative values");
        metadata.setFileSize(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, metadata.getFileSize(), "fileSize should accept max long value");

        // Edge values for strings
        metadata.setFileName("");
        assertEquals("", metadata.getFileName(), "fileName should accept empty string");
        metadata.setChecksum("");
        assertEquals("", metadata.getChecksum(), "checksum should accept empty string");
    }
}
