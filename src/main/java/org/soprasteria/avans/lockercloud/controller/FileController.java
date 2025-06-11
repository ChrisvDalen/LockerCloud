package org.soprasteria.avans.lockercloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/api/files")
@Tag(name = "File Operations", description = "Endpoints for file upload, download, deletion, listing and synchronization")
public class FileController {

    private final FileManagerService fileManagerService;

    @Autowired
    public FileController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @Operation(summary = "Upload a file", description = "Uploads a file to the server")
    @ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Error uploading file")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Checksum", required = false) String checksum) {
        Map<String, String> resp = new HashMap<>();
        try {
            if (checksum != null) {
                fileManagerService.saveFileTransactional(file, checksum);
            } else {
                fileManagerService.saveFileWithRetry(file);
            }
            resp.put("status", "ok");
            String storedChecksum = fileManagerService.getFileChecksum(file.getOriginalFilename());
            if (storedChecksum != null) {
                resp.put("checksum", storedChecksum);
            }
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @Operation(summary = "Download a file", description = "Downloads a file from the server")
    @ApiResponse(responseCode = "200", description = "File downloaded successfully")
    @ApiResponse(responseCode = "400", description = "Error downloading file")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("file") String fileName) {
        return doDownload(fileName);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFilePath(@PathVariable String fileName) {
        return doDownload(fileName);
    }

    private ResponseEntity<byte[]> doDownload(String fileName) {
        try {
            byte[] fileData = fileManagerService.getFile(fileName);
            String checksum = fileManagerService.getFileChecksum(fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileData.length))
                    .header("Checksum", checksum == null ? "" : checksum)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Download all files as ZIP",
           description = "Bundles all server files into a single ZIP and returns it")
    @ApiResponse(responseCode = "200", description = "ZIP downloaded successfully")
    @GetMapping("/downloadAll")
    public ResponseEntity<byte[]> downloadAllFiles() {
        try {
            List<String> filenames = fileManagerService.listFiles();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (String name : filenames) {
                    byte[] data = fileManagerService.getFile(name);
                    if (data == null) {
                        // Log a warning and skip this file
                        System.err.println("Warning: File " + name + " could not be found or is empty.");
                        continue;
                    }
                    ZipEntry entry = new ZipEntry(name);
                    zos.putNextEntry(entry);
                    zos.write(data);
                    zos.closeEntry();
                }
            }
            byte[] zipBytes = baos.toByteArray();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"all-files.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
        } catch (IOException | RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    @Operation(summary = "Delete a file", description = "Deletes a file from the server")
    @ApiResponse(responseCode = "200", description = "File deleted successfully")
    @ApiResponse(responseCode = "400", description = "Error deleting file")
    @RequestMapping(value = "/delete", method = {RequestMethod.DELETE, RequestMethod.GET})
    public ResponseEntity<String> deleteFile(
            @RequestParam(value = "file", required = false) String file,
            @RequestParam(value = "fileName", required = false) String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = file;
        }
        try {
            fileManagerService.deleteFile(fileName);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting file: " + e.getMessage());
        }
    }

    @Operation(summary = "List files", description = "Lists all files stored on the server")
    @ApiResponse(responseCode = "200", description = "Files listed successfully")
    @GetMapping("/list")
    public ResponseEntity<?> listFiles() {
        try {
            return ResponseEntity.ok(fileManagerService.listFiles());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error listing files: " + e.getMessage());
        }
    }

    @Operation(summary = "Synchronize files", description = "Synchronizes files between the client and server, returning instructions for upload, download, or conflict resolution")
    @ApiResponse(responseCode = "200", description = "Sync result returned successfully")
    @PostMapping("/sync")
    public ResponseEntity<?> syncFiles(@RequestBody List<FileMetadata> clientFiles) {
        try {
            SyncResult result = fileManagerService.syncFiles(clientFiles);
            if (!result.getConflictFiles().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(result);
            }
            return ResponseEntity.ok(result);
        } catch (FileStorageException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing files: " + e.getMessage());
        }
    }

    @GetMapping("/syncLocal")
    @Operation(summary = "Synchronize local client folder with server")
    public ResponseEntity<SyncResult> syncLocal() {
        try {
            SyncResult result = fileManagerService.performServerSideLocalSync();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error during server-side local sync: " + e.getMessage());

            List<String> emptyList = Collections.emptyList();
            List<String> errorConflict = Collections.singletonList("Server error during sync: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new SyncResult(emptyList, emptyList, errorConflict));
        }
    }
}
