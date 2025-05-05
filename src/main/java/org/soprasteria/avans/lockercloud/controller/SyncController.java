package org.soprasteria.avans.lockercloud.controller;


import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class SyncController {

    private final FileManagerService fileManagerService;

    public SyncController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping("/api/files/triggerSync")
    public CompletableFuture<SyncResult> triggerSync() {
        return fileManagerService.syncLocalClientFilesAsync();
    }
}
