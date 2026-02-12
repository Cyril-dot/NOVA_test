package com.novaTech.Nova.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticResourceController {
    
    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
    
    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    public ResponseEntity<Void> chromeDevTools() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}