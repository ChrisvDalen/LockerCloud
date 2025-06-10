package org.soprasteria.avans.lockercloud.controller;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("http")
public class HomeController {


    private final FileManagerService fileManagerService;

    public HomeController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Toon direct de huidige serverbestanden
        model.addAttribute("files", fileManagerService.listFiles());
        return "index";
    }
}
