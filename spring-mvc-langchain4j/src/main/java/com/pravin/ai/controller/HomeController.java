package com.pravin.ai.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    // GET /  -> home.jsp
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("serverTime",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        return "home";
    }

    // GET /health -> plain text "OK" (quick check that the app is up)
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }
}
