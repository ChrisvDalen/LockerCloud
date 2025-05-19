package org.soprasteria.avans.lockercloud.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileChunkTest {

    @Test
    void defaultConstructor_ShouldInitializeFieldsToDefaults() {
        FileChunk chunk = new FileChunk();

        assertNull(chunk.getData(), "Data should be null by default");
        assertEquals(0, chunk.getIndex(), "Index should be 0 by default");
        assertEquals(0, chunk.getTotalChunks(), "TotalChunks should be 0 by default");
        assertNull(chunk.getChecksum(), "Checksum should be null by default");
    }

    @Test
    void allArgsConstructor_ShouldSetAllFields() {
        byte[] data = new byte[]{1, 2, 3};
        int index = 5;
        int total = 10;
        String checksum = "abc123";

        FileChunk chunk = new FileChunk(data, index, total, checksum);

        assertSame(data, chunk.getData(), "getData should return the exact array instance");
        assertEquals(5, chunk.getIndex(), "Index should match constructor argument");
        assertEquals(10, chunk.getTotalChunks(), "TotalChunks should match constructor argument");
        assertEquals("abc123", chunk.getChecksum(), "Checksum should match constructor argument");
    }

    @Test
    void settersAndGetters_ShouldUpdateFields() {
        FileChunk chunk = new FileChunk();

        byte[] data = new byte[]{9, 8, 7};
        chunk.setData(data);
        chunk.setIndex(2);
        chunk.setTotalChunks(4);
        chunk.setChecksum("def456");

        assertSame(data, chunk.getData(), "setData/getData should store and return the same array");
        assertEquals(2, chunk.getIndex(), "setIndex/getIndex should store and return the same value");
        assertEquals(4, chunk.getTotalChunks(), "setTotalChunks/getTotalChunks should store and return the same value");
        assertEquals("def456", chunk.getChecksum(), "setChecksum/getChecksum should store and return the same string");
    }

    @Test
    void setters_ShouldAllowNullAndEdgeValues() {
        FileChunk chunk = new FileChunk();

        // null data
        chunk.setData(null);
        assertNull(chunk.getData(), "Data should be settable to null");

        // negative index and totalChunks
        chunk.setIndex(-1);
        chunk.setTotalChunks(-100);
        assertEquals(-1, chunk.getIndex(), "Index should accept negative values");
        assertEquals(-100, chunk.getTotalChunks(), "TotalChunks should accept negative values");

        // null checksum
        chunk.setChecksum(null);
        assertNull(chunk.getChecksum(), "Checksum should accept null");
    }
}
