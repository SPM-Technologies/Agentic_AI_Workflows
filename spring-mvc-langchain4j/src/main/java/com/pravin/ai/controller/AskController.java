package com.pravin.ai.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pravin.ai.service.AssistantService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AskController {

    /** One question + answer, shown on the page (Serializable: Tomcat may save sessions on restart). */
    public record ChatTurn(String question, String answer) implements Serializable { }

    private final AssistantService assistantService;

    // Spring passes in the AssistantService bean (constructor injection)
    public AskController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    // GET /ask -> show the conversation so far + the form
    @GetMapping("/ask")
    public String askForm(HttpSession session, Model model) {
        model.addAttribute("history", history(session));
        return "ask";
    }

    // POST /ask -> ask Claude, remember the turn, then show the page again.
    // The browser session id is the conversation id, so each browser has its own memory.
    @PostMapping("/ask")
    public String ask(@RequestParam String question, HttpSession session) {
        String answer = assistantService.answer(session.getId(), question);
        history(session).add(new ChatTurn(question, answer));
        return "redirect:/ask"; // refresh (F5) won't re-send the question
    }

    // POST /ask/clear -> "New chat": Claude and the page both forget the conversation
    @PostMapping("/ask/clear")
    public String clear(HttpSession session) {
        assistantService.clear(session.getId());
        session.removeAttribute("history");
        return "redirect:/ask";
    }

    @SuppressWarnings("unchecked")
    private List<ChatTurn> history(HttpSession session) {
        List<ChatTurn> history = (List<ChatTurn>) session.getAttribute("history");
        if (history == null) {
            history = new ArrayList<>();
            session.setAttribute("history", history);
        }
        return history;
    }
}
