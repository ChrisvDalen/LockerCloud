package org.soprasteria.avans.lockercloud.controller;


import org.soprasteria.avans.lockercloud.dto.SyncResult;
import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final FileManagerService fileManagerService;

    public HomeController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Voer automatische synchronisatie uit bij start
        SyncResult syncResult = fileManagerService.syncLocalClientFiles();
        model.addAttribute("syncResult", syncResult);
        // Toon ook de bestanden die op de server staan
        model.addAttribute("files", fileManagerService.listFiles());
        return "index";
    }
}
