package org.soprasteria.avans.lockercloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.model.FileMetadata;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Operations", description = "Endpoints for file upload, download, deletion, listing and synchronization")
public class FileController {

    private final FileManagerService fileManagerService;

    @Autowired
    public FileController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @Operation(summary = "Upload a file", description = "Uploads a file to the server")
    @ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Error uploading file")
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            fileManagerService.saveFileWithRetry(file);
            redirectAttributes.addFlashAttribute("uploadSuccess", "Bestand " + file.getOriginalFilename() + " succesvol geüpload!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("uploadError", "Fout bij uploaden: " + e.getMessage());
        }
        // Redirect terug naar de homepagina (index)
        return "redirect:/";
    }

    @Operation(summary = "Download a file", description = "Downloads a file from the server")
    @ApiResponse(responseCode = "200", description = "File downloaded successfully")
    @ApiResponse(responseCode = "400", description = "Error downloading file")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileName") String fileName) {
        try {
            byte[] fileData = fileManagerService.getFile(fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Delete a file", description = "Deletes a file from the server")
    @ApiResponse(responseCode = "200", description = "File deleted successfully")
    @ApiResponse(responseCode = "400", description = "Error deleting file")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("fileName") String fileName) {
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
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error syncing files: " + e.getMessage());
        }
    }
}
