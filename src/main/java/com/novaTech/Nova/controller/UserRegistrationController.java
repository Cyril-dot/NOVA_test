package com.novaTech.Nova.controller;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.TokenService;
import com.novaTech.Nova.Services.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;
    private final TokenService tokenService;

    private User getUserFromToken(String authHeader) {
        log.info("Auth header received {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header"
            );
        }

        String token = authHeader.substring(7);

        String email;
        try {
            email = tokenService.getEmailFromAccessToken(token);
        } catch (Exception e) {
            log.info("Invalid or expired token");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid or expired token"
            );
        }

        return userRegistrationService.findByEmail(email);
    }


    // =========================
    // Register User
    // =========================
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "mfaEnabled", defaultValue = "false") boolean mfaEnabled
    ) {
        try {
            UserRegistrationDTO dto = UserRegistrationDTO.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .username(username)
                    .password(password)
                    .confirmPassword(confirmPassword)
                    .profileImage(profileImage)
                    .mfaEnabled(mfaEnabled)
                    .build();

            UserResponseDTO response = userRegistrationService.createUser(dto);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "user", response
            ));

        } catch (RuntimeException | IOException e) {
            log.error("Error registering user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }



    // ========================
    // UPDATE AUTHENTICATED USER
    // ========================
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMyProfile(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader
    ) {
        try {
            com.novaTech.Nova.Entities.User user = userRegistrationService.getUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "Invalid authentication token"
                ));
            }

            UserRegistrationDTO dto = UserRegistrationDTO.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .username(username)
                    .email(email)
                    .password(password)
                    .confirmPassword(confirmPassword)
                    .profileImage(profileImage)
                    .build();

            UserResponseDTO response = userRegistrationService.updateUser(user.getId(), dto);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", response
            ));

        } catch (RuntimeException | IOException e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }



    // ========================
    // GET AUTHENTICATED USER DETAILS
    // ========================
    @GetMapping("/me")
    public ResponseEntity<?> getMyDetails(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            UserResponseDTO response = userRegistrationService.getAuthenticatedUserDetails(authHeader);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error fetching authenticated user details: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }



    // ========================
    // GET AUTHENTICATED USER'S PROFILE IMAGE
    // ========================
    @GetMapping("/me/profile-image")
    public ResponseEntity<byte[]> getMyProfileImage(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            byte[] imageBytes = userRegistrationService.getAuthenticatedUserProfileImage(authHeader);

            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = userRegistrationService.determineImageType(imageBytes);
            headers.setContentType(contentType);
            headers.setContentLength(imageBytes.length);
            headers.setCacheControl("max-age=3600, must-revalidate");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Error fetching authenticated user profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }




    // ========================
    // DELETE AUTHENTICATED USER'S PROFILE IMAGE
    // ========================
    @DeleteMapping("/me/profile-image")
    public ResponseEntity<?> deleteMyProfileImage(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            UserResponseDTO response = userRegistrationService.deleteAuthenticatedUserProfileImage(authHeader);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile image deleted successfully",
                    "user", response
            ));
        } catch (RuntimeException e) {
            log.error("Error deleting authenticated user profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }


    // to udpate password
    @PatchMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody ChangePasswordDTO dto) {
        log.info("Password change request received");
        log.info("Extracting user email from token");
        User user = getUserFromToken(authHeader);

        log.info("User email extracted successfully");
        String email = user.getEmail();
        String response = userRegistrationService.changePassword(email, dto);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/next/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        User user = getUserFromToken(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Fetch user within a transactional method to ensure lazy-loaded fields are accessible
        user = userRegistrationService.getUserById(user.getId());

        if (user == null || user.getProfileImage() == null || user.getProfileImage().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            // Decode Base64 stored in String
            byte[] imageBytes = Base64.getDecoder().decode(user.getProfileImage());

            HttpHeaders headers = new HttpHeaders();

            // Automatically determine content type from image data
            MediaType contentType = determineImageType(imageBytes);
            headers.setContentType(contentType);
            headers.setContentLength(imageBytes.length);

            // Add cache control for better performance
            headers.setCacheControl("max-age=3600, must-revalidate");

            // Allow cross-origin requests if needed
            headers.setAccessControlAllowOrigin("*");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error("Failed to decode profile image for user {}", user.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Determines the image type from the byte array magic numbers (file signatures)
     * Supports: JPEG, PNG, GIF, WebP, BMP, TIFF, ICO, SVG
     */
    private MediaType determineImageType(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return MediaType.APPLICATION_OCTET_STREAM; // Fallback for unknown
        }

        // JPEG: FF D8 FF
        if (imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 &&
                imageBytes[2] == (byte) 0xFF) {
            return MediaType.IMAGE_JPEG;
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (imageBytes.length >= 8 &&
                imageBytes[0] == (byte) 0x89 && imageBytes[1] == 0x50 &&
                imageBytes[2] == 0x4E && imageBytes[3] == 0x47 &&
                imageBytes[4] == 0x0D && imageBytes[5] == 0x0A &&
                imageBytes[6] == 0x1A && imageBytes[7] == 0x0A) {
            return MediaType.IMAGE_PNG;
        }

        // GIF: 47 49 46 38 (GIF87a or GIF89a)
        if (imageBytes.length >= 6 &&
                imageBytes[0] == 0x47 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x46 && imageBytes[3] == 0x38 &&
                (imageBytes[4] == 0x37 || imageBytes[4] == 0x39) &&
                imageBytes[5] == 0x61) {
            return MediaType.IMAGE_GIF;
        }

        // WebP: RIFF....WEBP
        if (imageBytes.length >= 12 &&
                imageBytes[0] == 0x52 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x46 && imageBytes[3] == 0x46 &&
                imageBytes[8] == 0x57 && imageBytes[9] == 0x45 &&
                imageBytes[10] == 0x42 && imageBytes[11] == 0x50) {
            return MediaType.parseMediaType("image/webp");
        }

        // BMP: 42 4D
        if (imageBytes[0] == 0x42 && imageBytes[1] == 0x4D) {
            return MediaType.parseMediaType("image/bmp");
        }

        // TIFF (Little Endian): 49 49 2A 00
        if (imageBytes.length >= 4 &&
                imageBytes[0] == 0x49 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x2A && imageBytes[3] == 0x00) {
            return MediaType.parseMediaType("image/tiff");
        }

        // TIFF (Big Endian): 4D 4D 00 2A
        if (imageBytes.length >= 4 &&
                imageBytes[0] == 0x4D && imageBytes[1] == 0x4D &&
                imageBytes[2] == 0x00 && imageBytes[3] == 0x2A) {
            return MediaType.parseMediaType("image/tiff");
        }

        // ICO: 00 00 01 00
        if (imageBytes.length >= 4 &&
                imageBytes[0] == 0x00 && imageBytes[1] == 0x00 &&
                imageBytes[2] == 0x01 && imageBytes[3] == 0x00) {
            return MediaType.parseMediaType("image/x-icon");
        }

        // SVG: Check for XML/SVG text signature
        if (imageBytes.length >= 5) {
            String start = new String(imageBytes, 0, Math.min(100, imageBytes.length), StandardCharsets.UTF_8);
            if (start.contains("<svg") || start.contains("<?xml")) {
                return MediaType.parseMediaType("image/svg+xml");
            }
        }

        // AVIF: Check for ftyp box with avif brand
        if (imageBytes.length >= 12 &&
                imageBytes[4] == 0x66 && imageBytes[5] == 0x74 &&
                imageBytes[6] == 0x79 && imageBytes[7] == 0x70) {
            String ftypBrand = new String(imageBytes, 8, 4, StandardCharsets.US_ASCII);
            if (ftypBrand.equals("avif") || ftypBrand.equals("avis")) {
                return MediaType.parseMediaType("image/avif");
            }
        }

        // HEIC/HEIF: Check for ftyp box with heic/heif brand
        if (imageBytes.length >= 12 &&
                imageBytes[4] == 0x66 && imageBytes[5] == 0x74 &&
                imageBytes[6] == 0x79 && imageBytes[7] == 0x70) {
            String ftypBrand = new String(imageBytes, 8, 4, StandardCharsets.US_ASCII);
            if (ftypBrand.equals("heic") || ftypBrand.equals("heix") ||
                    ftypBrand.equals("heif") || ftypBrand.equals("mif1")) {
                return MediaType.parseMediaType("image/heic");
            }
        }

        // Default fallback - treat as generic image
        log.warn("Unknown image format, using default content type");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    // =========================
    // Login without MFA
    // =========================
    @PostMapping("/login")
    public ResponseEntity<TokenDto> loginNoMfa(
            @RequestBody LoginRequest loginRequest
    ) {
        TokenDto token =
                userRegistrationService.loginNoMfa(loginRequest);

        return ResponseEntity.ok(token);
    }

    // =========================
    // Login with MFA
    // =========================
    @PostMapping("/login-mfa")
    public ResponseEntity<TokenDto> loginWithMfa(
            @RequestBody LoginRequest loginRequest
    ) {
        TokenDto token =
                userRegistrationService.loginWithMfa(loginRequest);

        return ResponseEntity.ok(token);
    }

    // =========================
    // Verify MFA code
    // =========================
    @PostMapping("/verify-mfa")
    public ResponseEntity<TokenDto> verifyMfa(
            @RequestParam String email,
            @RequestParam Integer mfaCode
    ) {
        TokenDto token =
                userRegistrationService.mfaVerify(email, mfaCode);

        return ResponseEntity.ok(token);
    }

    // =========================
    // View MFA code
    // =========================
    @GetMapping("/mfa-code")
    public ResponseEntity<Integer> viewMfaCode(
            @RequestParam String email
    ) {
        Integer code =
                userRegistrationService.viewMfaCode(email);

        return ResponseEntity.ok(code);
    }

    // =========================
    // Generate new MFA keys
    // =========================
    @PostMapping("/generate-mfa")
    public ResponseEntity<NewMfaKeys> generateMfaKeys(
            @RequestParam String email
    ) {
        NewMfaKeys keys =
                userRegistrationService.generateMfaKey(email);

        return ResponseEntity.ok(keys);
    }

    // =========================
    // View MFA details
    // =========================
    @GetMapping("/view-mfa")
    public ResponseEntity<NewMfaKeys> viewMfa(
            @RequestParam String email
    ) {
        NewMfaKeys keys =
                userRegistrationService.viewMfa(email);

        return ResponseEntity.ok(keys);
    }



    // =========================
    // Disable MFA
    // =========================
    @PostMapping("/disable-mfa")
    public ResponseEntity<String> disableMfa(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Disable MFA request received");
        log.info("Extracting user email from token");
        User user = getUserFromToken(authHeader);
        log.info("User email extracted successfully");
        String email = user.getEmail();
        String response = userRegistrationService.disableMFA(email);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/enable-mfa")
    public ResponseEntity<NewMfaKeys> enableMfa(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        log.info("Enabling mfa request received");
        log.info("Extracting user id from token");
        User user = getUserFromToken(authHeader);
        log.info("User id extracted successfully");
        UUID userId = user.getId();
        NewMfaKeys keys = userRegistrationService.enableMFA(userId);
        return ResponseEntity.ok(keys);
    }

}
