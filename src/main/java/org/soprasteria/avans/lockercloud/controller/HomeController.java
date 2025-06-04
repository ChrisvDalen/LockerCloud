package org.soprasteria.avans.lockercloud.controller;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.soprasteria.avans.lockercloud.model.FileInfo;
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
        // Toon direct de huidige serverbestanden
        var infos = fileManagerService.listFileInfos();
        model.addAttribute("files", infos);
        long totalSize = infos.stream().mapToLong(f -> f.getSize()).sum();
        model.addAttribute("totalSize", FileInfo.formatSize(totalSize));
        return "index";
    }
}
