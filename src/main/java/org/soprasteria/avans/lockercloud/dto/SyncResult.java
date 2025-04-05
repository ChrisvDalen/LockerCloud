package org.soprasteria.avans.lockercloud.dto;


import java.util.List;

public class SyncResult {
    private List<String> filesToUpload;
    private List<String> filesToDownload;
    private List<String> conflictFiles;

    public List<String> getFilesToUpload() {
        return filesToUpload;
    }

    public void setFilesToUpload(List<String> filesToUpload) {
        this.filesToUpload = filesToUpload;
    }

    public List<String> getFilesToDownload() {
        return filesToDownload;
    }

    public void setFilesToDownload(List<String> filesToDownload) {
        this.filesToDownload = filesToDownload;
    }

    public List<String> getConflictFiles() {
        return conflictFiles;
    }

    public void setConflictFiles(List<String> conflictFiles) {
        this.conflictFiles = conflictFiles;
    }
}
