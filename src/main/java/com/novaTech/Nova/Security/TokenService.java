package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.entity.RefreshToken;
import com.novaTech.Nova.Security.entity.RefreshTokenRepo;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepo refreshTokenRepo;
    private final TokenEncryptionService encryptionService;

    /**
     * Ensure HS256 key is 256 bits (32 bytes)
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new RuntimeException("JWT secret key must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ===================== ACCESS TOKEN =====================

    /**
     * Generate encrypted access token
     * Returns: Encrypted JWT (unreadable without decryption key)
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        // Step 1: Create plain JWT with user claims
        String plainToken = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Step 2: Encrypt the JWT
        String encryptedToken = encryptionService.encryptToken(plainToken);

        log.info("🔒 Generated encrypted access token for user: {}", user.getEmail());
        return encryptedToken;
    }

    // ===================== REFRESH TOKEN =====================

    /**
     * Generate encrypted refresh token and store in database
     */
    public RefreshToken generateRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        // Step 1: Create plain JWT for refresh token
        String plainRefreshToken = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("tokenType", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Step 2: Encrypt the refresh token
        String encryptedRefreshToken = encryptionService.encryptToken(plainRefreshToken);

        // Step 3: Store encrypted token in database
        Instant expiryInstant = Instant.now().plusMillis(refreshExpirationMs);
        refreshTokenRepo.upsertUserRefreshToken(user.getId(), encryptedRefreshToken, expiryInstant);

        log.info("🔒 Generated encrypted refresh token for user: {}", user.getEmail());

        return refreshTokenRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Failed to create refresh token for user"));
    }

    // ===================== VALIDATION =====================

    /**
     * Validate encrypted access token
     * Step 1: Decrypt token
     * Step 2: Validate JWT signature and expiration
     */
    public boolean validateAccessToken(String encryptedToken) {
        try {
            // Step 1: Decrypt the token
            String plainToken = encryptionService.decryptToken(encryptedToken);

            // Step 2: Validate JWT
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken);

            log.debug("✅ Token validated successfully");
            return true;

        } catch (ExpiredJwtException ex) {
            log.warn("⚠️ Token expired");
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid JWT token", ex);
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Token decryption failed", ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    /**
     * Validate encrypted refresh token
     */
    public boolean validateRefreshToken(String encryptedRefreshToken) {
        try {
            // Step 1: Check if token exists in database
            RefreshToken refreshToken = refreshTokenRepo.findByToken(encryptedRefreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            // Step 2: Check if token is expired
            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                refreshTokenRepo.delete(refreshToken);
                throw new RuntimeException("Refresh token expired");
            }

            // Step 3: Decrypt and validate JWT
            String plainToken = encryptionService.decryptToken(encryptedRefreshToken);
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken);

            log.debug("✅ Refresh token validated successfully");
            return true;

        } catch (ExpiredJwtException ex) {
            log.warn("⚠️ Refresh token expired");
            throw new RuntimeException("Refresh token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid refresh token", ex);
            throw new RuntimeException("Invalid refresh token", ex);
        } catch (Exception ex) {
            log.error("❌ Refresh token validation failed", ex);
            throw new RuntimeException("Invalid or corrupted refresh token", ex);
        }
    }

    // ===================== EXTRACT CLAIMS (WITH DECRYPTION) =====================

    /**
     * Extract email from encrypted access token
     */
    public String getEmailFromAccessToken(String encryptedToken) {
        try {
            // Step 1: Decrypt the token
            String plainToken = encryptionService.decryptToken(encryptedToken);

            // Step 2: Extract email from JWT
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken)
                    .getBody()
                    .getSubject();  // Email is the subject

        } catch (ExpiredJwtException ex) {
            log.error("❌ Token expired while extracting email", ex);
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid token while extracting email", ex);
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Failed to decrypt token", ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    /**
     * Extract user ID from encrypted access token
     */
    public Long getUserIdFromAccessToken(String encryptedToken) {
        try {
            // Step 1: Decrypt the token
            String plainToken = encryptionService.decryptToken(encryptedToken);

            // Step 2: Extract userId from claims
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken)
                    .getBody();

            return claims.get("userId", Long.class);

        } catch (ExpiredJwtException ex) {
            log.error("❌ Token expired while extracting userId", ex);
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid token while extracting userId", ex);
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Failed to decrypt token", ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    /**
     * Extract role from encrypted access token
     */
    public String getRoleFromAccessToken(String encryptedToken) {
        try {
            // Step 1: Decrypt the token
            String plainToken = encryptionService.decryptToken(encryptedToken);

            // Step 2: Extract role from claims
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken)
                    .getBody();

            return claims.get("role", String.class);

        } catch (ExpiredJwtException ex) {
            log.error("❌ Token expired while extracting role", ex);
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid token while extracting role", ex);
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Failed to decrypt token", ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    // ===================== TOKEN REFRESH =====================

    /**
     * Refresh access token using encrypted refresh token
     * Returns new encrypted access token
     */
    public String refreshAccessToken(String encryptedRefreshToken) {
        try {
            // Step 1: Validate refresh token
            validateRefreshToken(encryptedRefreshToken);

            // Step 2: Get user from refresh token
            RefreshToken refreshToken = refreshTokenRepo.findByToken(encryptedRefreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            User user = refreshToken.getUser();

            // Step 3: Generate new encrypted access token
            String newAccessToken = generateAccessToken(user);

            log.info("🔄 Access token refreshed for user: {}", user.getEmail());
            return newAccessToken;

        } catch (Exception ex) {
            log.error("❌ Failed to refresh access token", ex);
            throw new RuntimeException("Failed to refresh token", ex);
        }
    }

    // ===================== TOKEN REVOCATION =====================

    /**
     * Revoke (delete) refresh token
     */
    public void revokeRefreshToken(String encryptedRefreshToken) {
        try {
            refreshTokenRepo.findByToken(encryptedRefreshToken)
                    .ifPresent(token -> {
                        refreshTokenRepo.delete(token);
                        log.info("🗑️ Refresh token revoked for user: {}", token.getUser().getEmail());
                    });
        } catch (Exception ex) {
            log.error("❌ Failed to revoke refresh token", ex);
            throw new RuntimeException("Failed to revoke token", ex);
        }
    }

    /**
     * Revoke all refresh tokens for a user (e.g., on password change)
     */
    public void revokeAllUserRefreshTokens(User user) {
        try {
            refreshTokenRepo.findByUser(user).ifPresent(token -> {
                refreshTokenRepo.delete(token);
                log.info("🗑️ All refresh tokens revoked for user: {}", user.getEmail());
            });
        } catch (Exception ex) {
            log.error("❌ Failed to revoke user refresh tokens", ex);
            throw new RuntimeException("Failed to revoke tokens", ex);
        }
    }

    // ===================== HELPER METHODS =====================

    /**
     * Check if encrypted token is expired without throwing exception
     */
    public boolean isTokenExpired(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken)
                    .getBody();

            return claims.getExpiration().before(new Date());

        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            log.error("❌ Failed to check token expiration", ex);
            return true;
        }
    }

    /**
     * Get remaining time until token expiration (in milliseconds)
     */
    public long getTokenExpirationTime(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(plainToken)
                    .getBody();

            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();

            return Math.max(0, remainingTime);

        } catch (Exception ex) {
            log.error("❌ Failed to get token expiration time", ex);
            return 0;
        }
    }
}