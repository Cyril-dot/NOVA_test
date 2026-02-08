package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MfaService {

    private final GoogleAuthenticator gAuth;

    public MfaService() {
        // ±1 window to allow for clock drift (30s per window)
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(2) // allows 1 window before and after current
                .build();
        this.gAuth = new GoogleAuthenticator(config);
    }

    // ------------------- GENERATE MFA SECRET -------------------
    /**
     * Generates a new MFA secret key for the user
     * @return The secret key string to be stored in the database
     */
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();
        log.info("✅ Generated new MFA secret: {}", secret.substring(0, 4) + "****");
        return secret;
    }

    // ------------------- GENERATE CURRENT TOTP CODE -------------------
    /**
     * Generates the current TOTP code based on the user's MFA secret
     * This is the 6-digit code that the user would see in their authenticator app
     * @param user The user with MFA enabled
     * @return The current 6-digit TOTP code, or null if user has no secret
     */
    public Integer generateCurrentCode(User user) {
        if (user == null || user.getMfaSecret() == null || user.getMfaSecret().isEmpty()) {
            log.warn("⚠️ Cannot generate MFA code: user or secret is null");
            return null;
        }

        try {
            Integer code = gAuth.getTotpPassword(user.getMfaSecret());
            log.info("✅ Generated MFA code for user: {} - Code: {}", user.getEmail(), code);
            return code;
        } catch (Exception e) {
            log.error("❌ Failed to generate MFA code for user: {}. Error: {}", user.getEmail(), e.getMessage());
            return null;
        }
    }

    // ------------------- GENERATE CODE FROM SECRET -------------------
    /**
     * Generates the current TOTP code from a secret key
     * @param secret The MFA secret key
     * @return The current 6-digit TOTP code, or null if secret is invalid
     */
    public Integer generateCodeFromSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            log.warn("⚠️ Cannot generate MFA code: secret is null or empty");
            return null;
        }

        try {
            Integer code = gAuth.getTotpPassword(secret);
            log.info("✅ Generated MFA code from secret: {}", code);
            return code;
        } catch (Exception e) {
            log.error("❌ Failed to generate MFA code from secret. Error: {}", e.getMessage());
            return null;
        }
    }

    // ------------------- VERIFY MFA CODE -------------------
    /**
     * Verifies if the provided code matches the current TOTP code for the user
     * @param user The user with MFA enabled
     * @param code The code to verify (from user input)
     * @return true if the code is valid, false otherwise
     */
    public boolean verifyCode(User user, Integer code) {
        if (user == null || user.getMfaSecret() == null || code == null) {
            log.warn("⚠️ Cannot verify MFA code: user, secret, or code is null");
            return false;
        }

        try {
            boolean isValid = gAuth.authorize(user.getMfaSecret(), code);
            if (isValid) {
                log.info("✅ MFA code verified successfully for user: {}", user.getEmail());
            } else {
                log.warn("⚠️ Invalid MFA code provided for user: {}", user.getEmail());
            }
            return isValid;
        } catch (Exception e) {
            log.error("❌ Failed to verify MFA code for user: {}. Error: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    // ------------------- GENERATE QR CODE URL -------------------
    /**
     * Generates a QR code URL for the user to scan with their authenticator app
     * @param user The user
     * @param issuer The name of your application (e.g., "NOVA SPACE")
     * @return The QR code URL
     */
    public String generateQRCodeUrl(User user, String issuer) {
        if (user == null || user.getMfaSecret() == null) {
            log.warn("⚠️ Cannot generate QR code URL: user or secret is null");
            return null;
        }

        String accountName = user.getEmail(); // or username
        String url = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer,
                accountName,
                user.getMfaSecret(),
                issuer
        );

        log.info("✅ Generated QR code URL for user: {}", user.getEmail());
        return url;
    }
}