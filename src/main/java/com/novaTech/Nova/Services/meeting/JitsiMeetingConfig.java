package com.novaTech.Nova.Services.meeting;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JaaS (8x8.vc) integration.
 *
 * Required application.properties / environment variables:
 *
 *   jitsi.domain          = 8x8.vc
 *   jitsi.app.id          = vpaas-magic-cookie-7eca117a3c424c7bb5c5787891573dbf
 *   jitsi.api.key.id      = vpaas-magic-cookie-7eca117a3c424c7bb5c5787891573dbf/xxxxxx
 *   jitsi.private.key     = <base64 PKCS8 private key — no headers needed>
 *   jitsi.jwt.enabled     = true
 *   jitsi.room.expiry-hours = 2
 *
 * The private key value can be:
 *   - Raw base64 string (no PEM headers) — what JaaS downloads as
 *   - Full PEM block (-----BEGIN PRIVATE KEY----- ... -----END PRIVATE KEY-----)
 *   Both formats are handled by JitsiMeetingService#loadPrivateKey()
 */
@Configuration
@Getter
public class JitsiMeetingConfig {

    /** JaaS domain — always 8x8.vc */
    @Value("${jitsi.domain:8x8.vc}")
    private String domain;

    /** Your JaaS AppID from the console */
    @Value("${jitsi.app.id:}")
    private String appId;

    /**
     * API key ID in the format: <AppID>/<keyId>
     * e.g. vpaas-magic-cookie-7eca117a3c424c7bb5c5787891573dbf/abc123
     * This goes into the JWT header as "kid"
     */
    @Value("${jitsi.api.key.id:}")
    private String apiKeyId;

    /**
     * RS256 private key — either raw base64 or full PEM.
     * Store as env var JITSI_PRIVATE_KEY to keep it out of source control.
     */
    @Value("${jitsi.private.key:}")
    private String privateKey;

    /** Always true for JaaS — JWT is required */
    @Value("${jitsi.jwt.enabled:true}")
    private boolean jwtEnabled;

    /** Token lifetime in hours (default 2h) */
    @Value("${jitsi.room.expiry-hours:2}")
    private long roomExpiryHours;

    public long getRoomExpirySeconds() {
        return roomExpiryHours * 3600L;
    }

    /**
     * Builds the JaaS room URL.
     * Format: https://8x8.vc/<AppID>/<roomName>
     */
    public String buildRoomUrl(String roomName) {
        if (appId != null && !appId.isBlank()) {
            return "https://" + domain + "/" + appId + "/" + roomName;
        }
        // Fallback (shouldn't happen with JaaS)
        return "https://" + domain + "/" + roomName;
    }
}