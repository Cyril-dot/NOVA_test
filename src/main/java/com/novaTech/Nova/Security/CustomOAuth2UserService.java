package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.Enums.Role;
import com.novaTech.Nova.Entities.Enums.Status;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepo userRepo;

    public CustomOAuth2UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        log.info("OAuth2 authentication started - Attributes: {}", oAuth2User.getAttributes());

        // Provider: google / github
        String registrationId = userRequest
                .getClientRegistration()
                .getRegistrationId();

        log.info("OAuth2 Provider: {}", registrationId);

        // ---------------- Get Email ----------------
        String email = oAuth2User.getAttribute("email");

        // GitHub often returns null email → fallback
        if (email == null || email.isEmpty()) {
            String login = oAuth2User.getAttribute("login");
            email = login != null ? login + "@github.com" : "unknown@" + registrationId + ".com";
            log.warn("Email not provided by OAuth2 provider, using fallback: {}", email);
        }

        // ---------------- Get Provider ID ----------------
        String providerId;
        if ("google".equals(registrationId)) {
            providerId = oAuth2User.getAttribute("sub"); // Google uses "sub"
        } else if ("github".equals(registrationId)) {
            providerId = oAuth2User.getAttribute("id") != null
                    ? oAuth2User.getAttribute("id").toString()
                    : null;
        } else {
            providerId = email; // fallback
        }

        // ---------------- Get Name ----------------
        String name;
        String firstName;
        String lastName;

        if ("google".equals(registrationId)) {
            name = oAuth2User.getAttribute("name");
            firstName = oAuth2User.getAttribute("given_name");
            lastName = oAuth2User.getAttribute("family_name");

            // Fallback if given_name/family_name are null
            if (firstName == null && name != null) {
                String[] nameParts = name.split(" ", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            }
        } else if ("github".equals(registrationId)) {
            name = oAuth2User.getAttribute("name");
            String login = oAuth2User.getAttribute("login");

            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            } else {
                firstName = login != null ? login : "GitHub";
                lastName = "User";
            }
        } else {
            firstName = "Unknown";
            lastName = "User";
        }

        // Ensure firstName and lastName are never null
        firstName = firstName != null ? firstName : "Unknown";
        lastName = lastName != null ? lastName : "";

        // ---------------- Get Profile Picture (Optional) ----------------
        String pictureUrl = null;
        if ("google".equals(registrationId)) {
            pictureUrl = oAuth2User.getAttribute("picture");
        } else if ("github".equals(registrationId)) {
            pictureUrl = oAuth2User.getAttribute("avatar_url");
        }

        if (pictureUrl != null) {
            log.info("Profile picture URL: {}", pictureUrl);
            // TODO: Download and store profile image
        }

        // ---------------- Find or Create User ----------------
        String finalEmail = email;
        String finalFirstName = firstName;
        String finalLastName = lastName;
        String finalProviderId = providerId;

        User user = userRepo.findByEmail(email).orElseGet(() -> {
            log.info("Creating new OAuth2 user - Email: {}, Provider: {}", finalEmail, registrationId);

            User newUser = User.builder()
                    .email(finalEmail)
                    .username(finalEmail) // Use email as username
                    .firstName(finalFirstName)
                    .lastName(finalLastName)
                    .password(null) // OAuth2 users don't have passwords
                    .oauthProvider(registrationId.toUpperCase()) // "GOOGLE", "GITHUB"
                    .oauthProviderId(finalProviderId)
                    .role(Role.USER)
                    .status(Status.ACTIVE)
                    .mfaEnabled(false)
                    .createdAt(LocalDateTime.now())
                    .lastLogin(LocalDateTime.now())
                    .build();

            User savedUser = userRepo.save(newUser);
            log.info("OAuth2 user created successfully - ID: {}, Email: {}",
                    savedUser.getId(), savedUser.getEmail());
            return savedUser;
        });

        // ---------------- Update Last Login for Existing Users ----------------
        if (user.getId() != null && user.getCreatedAt() != null) {
            // Only update if user was already in database
            if (!user.getCreatedAt().equals(user.getLastLogin())) {
                user.updateLastLogin();
                userRepo.save(user);
                log.info("Updated last login for user: {}", user.getEmail());
            }
        }

        log.info("OAuth2 user loaded successfully - Email: {}, Role: {}",
                user.getEmail(), user.getRole());

        // ✅ FIX: Determine the correct name attribute key based on provider
        String nameAttributeKey;
        if ("google".equals(registrationId)) {
            nameAttributeKey = "sub"; // Google always has "sub"
        } else if ("github".equals(registrationId)) {
            nameAttributeKey = "id"; // GitHub always has "id"
        } else {
            nameAttributeKey = "sub"; // Default fallback
        }

        // ✅ FIX: Create a modified attributes map to ensure the name attribute exists
        Map<String, Object> modifiedAttributes = new HashMap<>(oAuth2User.getAttributes());

        // Ensure the email attribute is present (even if it's a fallback)
        if (!modifiedAttributes.containsKey("email") || modifiedAttributes.get("email") == null) {
            modifiedAttributes.put("email", email);
        }

        log.info("Using name attribute key: {}", nameAttributeKey);

        // ---------------- Return Security User ----------------
        return new DefaultOAuth2User(
                List.of(() -> "ROLE_" + user.getRole().name()),
                modifiedAttributes, // Use modified attributes
                nameAttributeKey    // Use provider-specific name attribute
        );
    }
}