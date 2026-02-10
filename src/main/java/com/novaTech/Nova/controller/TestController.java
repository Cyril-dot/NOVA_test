package com.novaTech.Nova.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * Returns a simple plain text message.
     * URL: GET /api/test/hello
     */
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello from Test Controller!";
    }

    /**
     * Returns a simple JSON message.
     * URL: GET /api/test/message
     */
    @GetMapping("/message")
    public ResponseEntity<?> getSimpleMessage() {
        return ResponseEntity.ok(Map.of("message", "This is another simple message for testing."));
    }
}
