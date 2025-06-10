package org.soprasteria.avans.lockercloud.controller;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("http")
public class FileViewController {

    private final FileManagerService fileManagerService;

    public FileViewController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping("/showCloudDirectory")
    public String showCloudDirectory(Model model) {
        // Haal de lijst van bestanden op via de service
        model.addAttribute("files", fileManagerService.listFiles());
        // Geef de naam van de Thymeleaf template terug (showCloudDirectory.html)
        return "showCloudDirectory";
    }
}

