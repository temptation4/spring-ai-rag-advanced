package com.example.springaidemo.controller;

import com.example.springaidemo.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * The caller identifies itself with the {@code X-User-Id} header; that value
     * becomes the chat-memory conversation id, so every user gets an independent
     * RAG conversation history. Missing header -> 400.
     */
    @GetMapping("/ask")
    public String ask(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String question) {

        return ragService.ask(userId, question);
    }
}
