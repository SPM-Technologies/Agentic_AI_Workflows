package com.pravin.ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pravin.ai.service.WorkflowService;

@Controller
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    // GET /workflows -> empty form, SEQUENCE selected
    @GetMapping("/workflows")
    public String form(Model model) {
        model.addAttribute("pattern", "SEQUENCE");
        return "workflows";
    }

    // POST /workflows -> run the chosen pattern and show the result + steps
    @PostMapping("/workflows")
    public String run(@RequestParam String pattern, @RequestParam String input, Model model) {
        model.addAttribute("pattern", pattern);
        model.addAttribute("input", input);
        model.addAttribute("result", workflowService.run(pattern, input));
        return "workflows";
    }
}
