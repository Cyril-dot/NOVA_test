package com.novaTech.Nova.controller;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/api/oauth2")  // ✅ moved under /api to avoid conflict with Spring's /oauth2/** internal paths
public class OAuthController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public OAuthController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/login/google")
    public RedirectView loginWithGoogle() {
        return new RedirectView("/oauth2/authorization/google");
    }

    @GetMapping("/login/github")
    public RedirectView loginWithGithub() {
        return new RedirectView("/oauth2/authorization/github");
    }
}