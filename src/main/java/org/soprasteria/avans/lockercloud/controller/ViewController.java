package org.soprasteria.avans.lockercloud.controller;

import org.soprasteria.avans.lockercloud.service.FileManagerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view")
public class ViewController {

    private final FileManagerService fileManagerService;

    public ViewController(FileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("files", fileManagerService.listFiles());
        return "index"; // renders index.html
    }
}

