package org.soprasteria.avans.lockercloud.dto;


import java.util.List;

public class SyncResult {
    private List<String> filesToUpload;
    private List<String> filesToDownload;
    private List<String> conflictFiles;

    public SyncResult() { }

    public SyncResult(List<String> filesToUpload, List<String> filesToDownload, List<String> conflictsFiles) {
        this.filesToUpload = filesToUpload;
        this.filesToDownload = filesToDownload;
        this.conflictFiles = conflictsFiles;
    }

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

    public int getUploadCount() {
        return filesToUpload.size();
    }
    public int getDownloadCount() {
        return filesToDownload.size();
    }
    public int getConflictCount() {
        return conflictFiles.size();
    }
}
