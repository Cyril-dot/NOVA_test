package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Security.entity.RefreshToken;
import com.novaTech.Nova.Security.entity.RefreshTokenRepo;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

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

    // Ensure HS256 key is 256 bits (32 bytes)
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new RuntimeException("JWT secret key must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ===================== ACCESS TOKEN =====================
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ===================== REFRESH TOKEN =====================
    public RefreshToken generateRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusMillis(refreshExpirationMs);

        // Upsert refresh token
        refreshTokenRepo.upsertUserRefreshToken(user.getId(), tokenValue, expiry);

        return refreshTokenRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Failed to create refresh token for user"));
    }

    // ===================== VALIDATION =====================
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            throw new RuntimeException("Invalid token", ex);
        }
    }

    // ===================== EXTRACT CLAIMS =====================
    public String getEmailFromAccessToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();  // Email is the subject
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            throw new RuntimeException("Invalid token", ex);
        }
    }

    public Long getUserIdFromAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("userId", Long.class);
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            throw new RuntimeException("Invalid token", ex);
        }
    }

    public String getRoleFromAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("role", String.class);
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            throw new RuntimeException("Invalid token", ex);
        }
    }
}