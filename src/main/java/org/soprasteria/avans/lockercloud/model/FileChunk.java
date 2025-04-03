package org.soprasteria.avans.lockercloud.model;

public class FileChunk {
    private byte[] data;
    private int index;
    private int totalChunks;
    private String checksum;

    public FileChunk() {
    }

    public FileChunk(byte[] data, int index, int totalChunks, String checksum) {
        this.data = data;
        this.index = index;
        this.totalChunks = totalChunks;
        this.checksum = checksum;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}

