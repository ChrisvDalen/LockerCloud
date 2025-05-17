package org.soprasteria.avans.lockercloud.model;

import java.time.LocalDateTime;

public class FileMetadata {
    private String fileName;
    private String checksum;
    private long fileSize;
    private LocalDateTime uploadDate;
    private long lastModified;

    public FileMetadata() { }

    public FileMetadata(String fileName, String checksum, long fileSize, LocalDateTime uploadDate, long lastModified) {
        this.fileName = fileName;
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.uploadDate = uploadDate;
        this.lastModified = lastModified;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
