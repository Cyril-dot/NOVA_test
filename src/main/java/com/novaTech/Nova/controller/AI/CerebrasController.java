package com.novaTech.Nova.controller.AI;


import com.novaTech.Nova.Services.AI.CerebrasService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/external/cerebras")
public class CerebrasController {

    private final CerebrasService cerebrasService;

    public CerebrasController(CerebrasService cerebrasService) {
        this.cerebrasService = cerebrasService;
    }

    @PostMapping("/chat")
    public String chatWithCerebras(@RequestParam String message) {
        return cerebrasService.sendChat(message);
    }
}
