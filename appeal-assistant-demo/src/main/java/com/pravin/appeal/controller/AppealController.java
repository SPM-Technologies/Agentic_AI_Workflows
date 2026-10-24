package com.pravin.appeal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.pravin.appeal.data.DemoData;
import com.pravin.appeal.data.DemoData.DeniedClaim;
import com.pravin.appeal.service.AppealService;

@Controller
public class AppealController {

    /** Demo user - a real product would take this from the login session. */
    private static final String USER = "biller.demo";

    private final DemoData data;
    private final AppealService appealService;

    public AppealController(DemoData data, AppealService appealService) {
        this.data = data;
        this.appealService = appealService;
    }

    // GET / -> the biller's work list of denied claims
    @GetMapping("/")
    public String worklist(Model model) {
        model.addAttribute("claims", data.claims());
        model.addAttribute("appeals", appealService.all());
        model.addAttribute("data", data);
        return "worklist";
    }

    // POST /claims/{id}/prepare -> start the AI workflow in the background, go to the review page
    @PostMapping("/claims/{claimId}/prepare")
    public String prepare(@PathVariable String claimId) {
        claim(claimId);
        appealService.prepare(claimId, USER);
        return "redirect:/appeals/" + claimId;
    }

    // GET /appeals/{id} -> review screen (refreshes itself while the AI is working)
    @GetMapping("/appeals/{claimId}")
    public String review(@PathVariable String claimId, Model model) {
        DeniedClaim claim = claim(claimId);
        model.addAttribute("claim", claim);
        model.addAttribute("patient", data.patient(claim.patientId()));
        model.addAttribute("appeal", appealService.find(claimId));
        return "appeal";
    }

    // POST /appeals/{id}/approve -> human approval (with possibly edited text)
    @PostMapping("/appeals/{claimId}/approve")
    public String approve(@PathVariable String claimId, @RequestParam String finalText) {
        claim(claimId);
        appealService.approve(claimId, finalText, USER);
        return "redirect:/appeals/" + claimId;
    }

    // POST /appeals/{id}/reject -> human rejection with a reason
    @PostMapping("/appeals/{claimId}/reject")
    public String reject(@PathVariable String claimId, @RequestParam String reason) {
        claim(claimId);
        appealService.reject(claimId, reason, USER);
        return "redirect:/appeals/" + claimId;
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "OK";
    }

    private DeniedClaim claim(String claimId) {
        return data.claim(claimId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown claim"));
    }

    // e.g. approving something that is not waiting for review, or re-preparing an appeal already sent
    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public ResponseEntity<String> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    // e.g. empty approved text or empty rejection reason
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
