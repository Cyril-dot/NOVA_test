package com.novaTech.Nova.Services.meeting;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Jitsi Meet integration.
 *
 * Required in your .env / deployment environment:
 *   JITSI_DOMAIN        → e.g. meet.jit.si  (or your self-hosted domain)
 *   JITSI_APP_ID        → your Jitsi app id (if using jaas.8x8.vc)
 *   JITSI_API_KEY_ID    → key id for JWT signing
 *   JITSI_PRIVATE_KEY   → RS256 private key PEM (for JWT, optional for public meet.jit.si)
 *
 * For public meet.jit.si, only JITSI_DOMAIN is required.
 */
@Configuration
@Getter
public class JitsiMeetingConfig {

    @Value("${jitsi.domain:meet.jit.si}")
    private String domain;

    /** App ID — required only for JaaS (8x8.vc). Leave blank for public meet.jit.si */
    @Value("${jitsi.app.id:}")
    private String appId;

    /** API key id — required only for JaaS JWT auth */
    @Value("${jitsi.api.key.id:}")
    private String apiKeyId;

    /** RS256 private key PEM — required only for JaaS JWT auth */
    @Value("${jitsi.private.key:}")
    private String privateKey;

    /** Whether JWT tokens are required (true for JaaS, false for public meet.jit.si) */
    @Value("${jitsi.jwt.enabled:false}")
    private boolean jwtEnabled;

    /** Room expiry in seconds (used for JWT exp claim) */
    @Value("${jitsi.room.expiry-hours:24}")
    private long roomExpiryHours;

    public long getRoomExpirySeconds() {
        return roomExpiryHours * 3600L;
    }

    public String buildRoomUrl(String roomName) {
        if (appId != null && !appId.isBlank()) {
            // JaaS URL format
            return "https://" + domain + "/" + appId + "/" + roomName;
        }
        return "https://" + domain + "/" + roomName;
    }
}