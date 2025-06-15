package org.soprasteria.avans.lockercloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.exception.FileStorageException;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.soprasteria.avans.lockercloud.socket.SocketFileClient;
import org.soprasteria.avans.lockercloud.socket.SocketFileClient.DownloadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileManagerService fileManagerService;
    private final String socketHost;
    private final int socketPort;

    @Autowired
    public FileController(FileManagerService fileManagerService,
                          @Value("${file.socket.host:localhost}") String socketHost,
                          @Value("${file.socket.port:9090}") int socketPort) {
        this.fileManagerService = fileManagerService;
        this.socketHost = socketHost;
        this.socketPort = socketPort;
    }


    @GetMapping("/")
    public String index() {
        return "index";
    }

    @Operation(summary = "Upload a file", description = "Uploads a file to the server")
    @ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Error uploading file")
    @PostMapping("/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "Checksum", required = false) String checksum) {
        try (SocketFileClient client = new SocketFileClient(socketHost, socketPort)) {
            log.info("Uploading {} via socket", file.getOriginalFilename());
            client.upload(file.getOriginalFilename(), file.getBytes());
            redirectAttributes.addFlashAttribute("uploadSuccess",
                    "Bestand " + file.getOriginalFilename() + " succesvol geüpload!");
        } catch (Exception e) {
            log.error("Socket upload failed", e);
            redirectAttributes.addFlashAttribute("uploadError",
                    "Fout bij uploaden: " + e.getMessage());
        }
        return "redirect:/";
    }

    @Operation(summary = "Download a file", description = "Downloads a file from the server")
    @ApiResponse(responseCode = "200", description = "File downloaded successfully")
    @ApiResponse(responseCode = "400", description = "Error downloading file")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(
            @RequestParam(value = "file", required = false) String file,
            @RequestParam(value = "fileName", required = false) String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = file;
        }
        return doDownload(fileName);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFilePath(@PathVariable String fileName) {
        return doDownload(fileName);
    }

    private ResponseEntity<byte[]> doDownload(String fileName) {
        try (SocketFileClient client = new SocketFileClient(socketHost, socketPort)) {
            DownloadResult result = client.download(fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(result.data.length))
                    .header("Checksum", result.checksum == null ? "" : result.checksum)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(result.data);
        } catch (Exception e) {
            log.error("Download via socket failed", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Download all files as ZIP",
           description = "Bundles all server files into a single ZIP and returns it")
    @ApiResponse(responseCode = "200", description = "ZIP downloaded successfully")
    @GetMapping("/downloadAll")
    public ResponseEntity<byte[]> downloadAllFiles() {
        try {
            List<String> filenames;
            try {
                filenames = fileManagerService.listFiles();
            } catch (Exception e) {
                System.err.println("Warning: could not list files: " + e.getMessage());
                filenames = Collections.emptyList();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (String name : filenames) {
                    try {
                        byte[] data = fileManagerService.getFile(name);
                        if (data == null) {
                            System.err.println("Warning: File " + name + " could not be found or is empty.");
                            continue;
                        }
                        ZipEntry entry = new ZipEntry(name);
                        zos.putNextEntry(entry);
                        zos.write(data);
                        zos.closeEntry();
                    } catch (Exception ex) {
                        System.err.println("Warning: Failed to add " + name + " to ZIP: " + ex.getMessage());
                    }
                }
            }
            byte[] zipBytes = baos.toByteArray();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"all-files.zip\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipBytes.length))
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(zipBytes);
        } catch (IOException e) {
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
        try (SocketFileClient client = new SocketFileClient(socketHost, socketPort)) {
            log.info("Deleting {} via socket", fileName);
            client.delete(fileName);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            log.error("Delete via socket failed", e);
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
