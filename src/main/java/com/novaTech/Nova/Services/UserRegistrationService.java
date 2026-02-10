package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.Role;
import com.novaTech.Nova.Entities.Enums.Status;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Security.MfaService;
import com.novaTech.Nova.Security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepo userRepo;
    private final TokenService tokenService;
    private final MfaService mfaService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    // ========================
// CREATE USER
// ========================
    public UserResponseDTO createUser(UserRegistrationDTO dto) throws IOException {

        if (userRepo.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());

        // Password validation and encoding
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // ===== Base64 profile image storage =====
        MultipartFile profileImageFile = dto.getProfileImage();
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            try {
                // Validate file size (10MB max)
                if (profileImageFile.getSize() > 10 * 1024 * 1024) {
                    throw new RuntimeException("Profile image must be less than 10MB");
                }

                // Validate it's actually an image
                String contentType = profileImageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new RuntimeException("File must be an image");
                }

                // Read the actual file bytes
                byte[] imageBytes = profileImageFile.getBytes();

                // Verify we have actual data
                if (imageBytes == null || imageBytes.length == 0) {
                    throw new RuntimeException("Profile image file is empty");
                }

                // Convert to Base64 string for storage
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Log for debugging
                log.info("Storing profile image for user {}: Original size: {} bytes, Base64 length: {} chars",
                        dto.getEmail(), imageBytes.length, base64Image.length());

                user.setProfileImage(base64Image);

            } catch (IOException e) {
                log.error("Failed to process profile image for user {}", dto.getEmail(), e);
                throw new RuntimeException("Failed to process profile image: " + e.getMessage());
            }
        }

        user.setMfaEnabled(dto.isMfaEnabled());
        if (dto.isMfaEnabled()) {
            user.setMfaSecret(mfaService.generateSecret());
            user.setMfaCode(mfaService.generateCurrentCode(user));
            user.setGeneratedAt(LocalDateTime.now());

            try {
                log.info("Sending OTP email to: {}", user.getEmail());
                emailService.sendOtpEmail(user.getEmail(), user.getMfaCode());
                log.info("OTP email sent successfully to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send OTP email to: {}. Error: {}",
                        user.getEmail(), e.getMessage(), e);
                // User is still created successfully even if email fails
            }
        }

        user.setCreatedAt(LocalDateTime.now());
        user.setStatus(Status.ACTIVE);
        user.setRole(Role.USER);

        User savedUser = userRepo.save(user);

        // Send email AFTER transaction completes (wrapped in try-catch)
        try {
            log.info("Sending account creation email to: {}", savedUser.getEmail());
            emailService.sendAccountCreationEmail(savedUser.getEmail());
            log.info("Account creation email sent successfully to: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send account creation email to: {}. Error: {}",
                    savedUser.getEmail(), e.getMessage(), e);
            // User is still created successfully even if email fails
        }

        // Verify the image was saved correctly
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            log.info("Profile image saved in DB for user {}: {} characters",
                    savedUser.getEmail(),
                    savedUser.getProfileImage() != null ? savedUser.getProfileImage().length() : 0);
        }

        return buildUserResponse(savedUser);
    }

    // ========================
    // UPDATE USER
    // ========================
    public UserResponseDTO updateUser(UUID userId, UpdateUserDTO dto) throws IOException {
        User user = getUserById(userId);
        if (user == null) throw new RuntimeException("User does not exist");

        // Update fields only if provided (not null/blank)
        if (dto.firstName() != null && !dto.firstName().isBlank()) {
            user.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null && !dto.lastName().isBlank()) {
            user.setLastName(dto.lastName());
        }
        if (dto.username() != null && !dto.username().isBlank()) {
            user.setUsername(dto.username());
        }
        if (dto.email() != null && !dto.email().isBlank()) {
            user.setEmail(dto.email());
        }

        // ===== Profile image update =====
        MultipartFile profileImageFile = dto.profileImage();
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            try {
                // Validate file size (10MB max)
                if (profileImageFile.getSize() > 10 * 1024 * 1024) {
                    throw new RuntimeException("Profile image must be less than 10MB");
                }

                // Validate it's actually an image
                String contentType = profileImageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new RuntimeException("File must be an image");
                }

                // Read the actual file bytes
                byte[] imageBytes = profileImageFile.getBytes();

                // Verify we have actual data
                if (imageBytes == null || imageBytes.length == 0) {
                    throw new RuntimeException("Profile image file is empty");
                }

                // Convert to Base64 string for storage
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Log for debugging
                log.info("Updating profile image for user {}: Original size: {} bytes, Base64 length: {} chars",
                        user.getId(), imageBytes.length, base64Image.length());

                user.setProfileImage(base64Image);

            } catch (IOException e) {
                log.error("Failed to process profile image for user {}", user.getId(), e);
                throw new RuntimeException("Failed to process profile image: " + e.getMessage());
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepo.save(user);

        // Verify the image was saved correctly
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            log.info("Profile image updated in DB for user {}: {} characters",
                    savedUser.getId(),
                    savedUser.getProfileImage() != null ? savedUser.getProfileImage().length() : 0);
        }

        return buildUserResponse(savedUser);
    }
    // ========================
    // GET PROFILE IMAGE
    // ========================
    @Transactional(readOnly = true)
    public byte[] getProfileImage(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfileImage() == null || user.getProfileImage().isEmpty()) {
            throw new RuntimeException("User has no profile image");
        }

        try {
            // Decode Base64 to bytes
            byte[] imageBytes = Base64.getDecoder().decode(user.getProfileImage());
            log.info("Retrieved profile image for user {}: {} bytes", userId, imageBytes.length);
            return imageBytes;
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode profile image for user {}", userId, e);
            throw new RuntimeException("Invalid profile image data");
        }
    }

    // ========================
    // GET USER DETAILS
    // ========================
    @Transactional(readOnly = true)
    public UserResponseDTO getAuthenticatedUser(String email) {
        User user = userRepo.findByUserEmail(email);
        if (user == null) {
            log.error("User does not exist");
            throw new RuntimeException("Invalid authentication token");
        }

        return buildUserResponse(user);
    }


    // ========================
    // GET AUTHENTICATED USER PROFILE IMAGE
    // ========================
    @Transactional(readOnly = true)
    public byte[] getAuthenticatedUserProfileImage(String email) {
        User user = userRepo.findByUserEmail(email);
        if (user == null) {
            log.error("User does not exist");
            throw new RuntimeException("Invalid authentication token");
        }

        return getProfileImage(user.getId());
    }

    // ========================
    // DELETE PROFILE IMAGE
    // ========================
    @Transactional
    public UserResponseDTO deleteProfileImage(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setProfileImage(null);
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepo.save(user);

        log.info("Profile image deleted for user {}", userId);
        return buildUserResponse(savedUser);
    }

    // ========================
    // DELETE AUTHENTICATED USER PROFILE IMAGE
    // ========================
    @Transactional
    public UserResponseDTO deleteAuthenticatedUserProfileImage(String email) {
        User user = userRepo.findByUserEmail(email);
        if (user == null) {
            log.error("User does not exist");
            throw new RuntimeException("Invalid authentication token");
        }

        return deleteProfileImage(user.getId());
    }

    // ========================
    // HELPER: Determine Image Type
    // ========================
    public MediaType determineImageType(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            return MediaType.APPLICATION_OCTET_STREAM;
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

        // GIF: 47 49 46 38
        if (imageBytes.length >= 6 &&
                imageBytes[0] == 0x47 && imageBytes[1] == 0x49 &&
                imageBytes[2] == 0x46 && imageBytes[3] == 0x38) {
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

        // Default fallback
        return MediaType.IMAGE_PNG;
    }

    private UserResponseDTO buildUserResponse(User user) {
        return UserResponseDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .mfaEnabled(user.isMfaEnabled())
                .mfaSecret(user.isMfaEnabled() ? user.getMfaSecret() : "MFA not enabled")
                .mfaCode(user.isMfaEnabled() ? user.getMfaCode() : null)
                .generatedAt(user.getGeneratedAt())
                .profileImage(user.getProfileImage()) // attach image here
                .build();
    }


    // now for login without mfa enabled
    public TokenDto loginNoMfa(LoginRequest loginRequest){
        // to check if mfa is enabled then reject
        User user = userRepo.findByEmail(loginRequest.email())
                .orElseThrow(() -> {
                    log.warn("User not found @ {}", loginRequest.email());
                    return new RuntimeException("User not found");
                });

        // now to check if mfa is enabled
        if (user.isMfaEnabled()){
            log.warn("MFA is enabled for user, verify mfa to enable");
            // Do NOT throw an exception if you want to return a response
            return new TokenDto(
                    null,
                    null,
                    "MFA is enabled for user, verify your mfa to login successfully"
            );
        }

        // to verify password
        if (!verifyPassword(user, loginRequest.password())){
            log.warn("Invalid password for user @ {}", loginRequest.email());
            throw new RuntimeException("Passwords do not match, Invalid password");
        }

        // to generate the token
        log.info("Access token generated successfully for user @ {}", user.getEmail());
        String accessToken = tokenService.generateAccessToken(user);

        log.info("Generating refresh token for user @ {}", user.getEmail());
        String refreshToken = tokenService.generateRefreshToken(user).getToken();
        user.setLastLogin(LocalDateTime.now());
        userRepo.save(user);

        return new TokenDto(accessToken, refreshToken, "Login successful");
    }

    //to update password
    public String changePassword(String email, ChangePasswordDTO dto) {

        User user = findByEmail(email);

        // Verify current password
        if (!passwordEncoder.matches(dto.currentPassword(), user.getPassword())) {
            log.warn("Password change failed: incorrect current password for {}", email);
            throw new RuntimeException("Current password is incorrect");
        }

        // Check new password match
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            log.warn("Password change failed: new passwords do not match for {}", email);
            throw new RuntimeException("New passwords do not match");
        }

        // Encode and update password
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepo.save(user);


            log.info("Sending password update notification email to: {}", user.getEmail());
            emailService.passwordUpdate(user.getEmail());
            log.info("Password update email sent successfully to: {}", user.getEmail());

        log.info("Password updated successfully for {}", email);
        return "Password updated successfully";
    }

    // now to verify mfa to get a token
    public TokenDto mfaVerify(String userEmail, Integer mfaCode){

        User user = userRepo.findByEmailAndMfaCode(userEmail, mfaCode)
                .orElseThrow(() -> {
                    log.warn("User not found @ {}", userEmail);
                    return new RuntimeException("User not found");
                });

        // now to check if mfa is enabled and the user is allowed to verify the mfa
        if(!user.isMfaEnabled()){
            log.warn("MFA is not enabled for user @ {}", userEmail);
            throw new RuntimeException("MFA is not enabled for this user, You are accessing a restricted resource");
        }

            log.info("Sending MFA verification success email to: {}", user.getEmail());
            emailService.mfaVerificationSuccessful(user.getEmail());
            log.info("MFA verification email sent successfully to: {}", user.getEmail());


        //to generate tokens
        log.info("Access token generated successfully for user @ {}", user.getEmail());
        String accessToken = tokenService.generateAccessToken(user);
        log.info("Generating refresh token for user @ {}", user.getEmail());
        String refreshToken = tokenService.generateRefreshToken(user).getToken();
        user.setLastLogin(LocalDateTime.now());
        userRepo.save(user);

        return new TokenDto(accessToken, refreshToken, "Login successful");
    }

    // now to view mfa code
    public Integer viewMfaCode(String userEmail){
        User user = findByEmail(userEmail);
        if (user == null){
            log.warn("User not found @ {}", userEmail);
            throw new RuntimeException("User not found");
        }

        if (!user.isMfaEnabled()){
            log.warn("MFA is not enabled for user @ {}", userEmail);
            throw new RuntimeException("MFA is not enabled for this user, You are accessing a restricted resource");
        }

        return user.getMfaCode();
    }

    // to generate a new mfa code and secret
    public NewMfaKeys generateMfaKey(String userEmail){
        log.info("Generating new set of mfa keys for user @ {}", userEmail);
        User user = findByEmail(userEmail);
        if (user == null){
            log.warn("User not found @ {}", userEmail);
            throw new RuntimeException("User not found");
        }

        if (!user.isMfaEnabled()){
            log.warn("MFA is not enabled for user @ {}", userEmail);
            throw new RuntimeException("MFA is not enabled for this user, You are accessing a restricted resource");
        }

        log.info("Generating new mfa secret for user");
        user.setMfaSecret(mfaService.generateSecret());
        log.info("Generating new mfa code for user");
        user.setMfaCode(mfaService.generateCurrentCode(user));
        user.setMfaUpdatedAt(LocalDateTime.now());


            log.info("Sending new OTP email to: {}", user.getEmail());
            emailService.sendOtpEmail(user.getEmail(), user.getMfaCode());
            log.info("OTP email sent successfully to: {}", user.getEmail());


        log.info("Mfa generated successfully for user @ {}", userEmail);
        userRepo.save(user);

        return new NewMfaKeys(user.getMfaSecret(), user.getMfaCode());
    }

// ✅ COMPLETE FIXED VERSION - UserRegistrationService.enableMFA()

    public NewMfaKeys enableMFA(UUID userId) {
        log.info("Enable MFA request for userId: {}", userId);

        User user = getUserById(userId);

        if (!user.isMfaEnabled()) {
            log.info("MFA currently disabled for userId: {}. Enabling MFA.", userId);

            // ✅ STEP 1: Generate the secret
            String secret = mfaService.generateSecret();

            // ✅ STEP 2: SET THE SECRET ON THE USER FIRST!
            user.setMfaSecret(secret);

            // ✅ STEP 3: NOW generate the code (after secret is set!)
            Integer code = mfaService.generateCurrentCode(user);

            // ✅ STEP 4: Set the code
            user.setMfaCode(code);
            user.setMfaEnabled(true);
            user.setGeneratedAt(LocalDateTime.now());
            user.setMfaUpdatedAt(LocalDateTime.now());

            // ✅ STEP 5: Save to database
            userRepo.save(user);

            // ✅ STEP 6: Send email asynchronously (won't block transaction)
            log.info("Sending MFA enabled notification email to: {}", user.getEmail());
            emailService.mfaEnabledMail(user.getEmail());
            emailService.sendOtpEmail(user.getEmail(), user.getMfaCode());
            log.info("MFA enabled successfully for userId: {}", userId);
        } else {
            log.info("MFA already enabled for userId: {}", userId);
        }

        return new NewMfaKeys(user.getMfaSecret(), user.getMfaCode());
    }


    public String disableMFA(String userEmail) {
        log.info("Disable MFA request for user email @: {}", userEmail);

        User user = findByEmail(userEmail);

        if (user == null) {
            log.warn("User not found with email: {}", userEmail);
            throw new RuntimeException("User not found");
        }

        if (user.isMfaEnabled()) {
            log.info("MFA currently enabled for user with email: {}. Disabling MFA.", userEmail);

            user.setMfaEnabled(false);
            user.setMfaSecret(null);
            user.setMfaCode(null);
            user.setGeneratedAt(null);
            user.setMfaUpdatedAt(null);
            userRepo.save(user);

                log.info("Sending MFA disabled notification email to: {}", user.getEmail());
                emailService.mfaDisabled(user.getEmail());
                log.info("MFA disabled email sent successfully to: {}", user.getEmail());

            log.info("MFA disabled successfully for userEmail: {}", userEmail);
        } else {
            log.info("MFA already disabled for userEmail: {}", userEmail);
        }

        log.info("Disable MFA response for userEmail: {}", userEmail);
        return "MFA disabled successfully";
    }

    public NewMfaKeys viewMfa(String email) {
        log.info("View MFA details request for user email: {}", email);

        User user = findByEmail(email);
        if (user == null) {
            log.warn("User not found with email: {}", email);
            throw new RuntimeException("User not found");
        }

        if (!user.isMfaEnabled()) {
            log.warn("Attempt to view MFA details but MFA is disabled for user with email: {}", email);
            throw new RuntimeException("MFA is not enabled for this user.");
        }

        log.info("MFA details retrieved successfully for user email: {}", email);

        return new NewMfaKeys(user.getMfaSecret(), user.getMfaCode());
    }

    // login with mfa enabled
    public TokenDto loginWithMfa(LoginRequest request){
        log.info("Login with MFA attempt for email: {}", request.email());

        User user = findByEmail(request.email());
        if (user == null){
            log.warn("Login with MFA failed: user not found for email: {}", request.email());
            throw new RuntimeException("User does not exist");
        }

        log.debug("User found for MFA login, email: {}", request.email());

        // to check if mfa is enabled
        if (!user.isMfaEnabled()){
            log.warn("Login with MFA failed: MFA not enabled for email: {}", request.email());
            throw new RuntimeException("MFA is not enabled for this user");
        }

        log.info("MFA is enabled for email: {}", request.email());

        // password check
        if (!verifyPassword(user, request.password())){
            log.warn("Login with MFA failed: invalid password for email: {}", request.email());
            throw new RuntimeException("Invalid password");
        }

        log.info("Password verified successfully for MFA login, email: {}", request.email());

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user).getToken();

        log.info("Tokens generated successfully for MFA login, email: {}", request.email());
        log.info("Login with MFA successful for email: {}", request.email());

        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .build();
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public User getUserById(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User with email " + email + " not found"));
    }
}