package org.soprasteria.avans.lockercloud;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileManagerService fileManagerService;

    @Autowired
    public FileController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            fileManagerService.saveFile(file);
            return ResponseEntity.ok("File uploaded successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error uploading file: " + e.getMessage());
        }
    }

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

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("fileName") String fileName) {
        try {
            fileManagerService.deleteFile(fileName);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting file: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listFiles() {
        try {
            return ResponseEntity.ok(fileManagerService.listFiles());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error listing files: " + e.getMessage());
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncFiles() {
        try {
            fileManagerService.syncFiles();
            return ResponseEntity.ok("Files synchronized successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error syncing files: " + e.getMessage());
        }
    }
}
