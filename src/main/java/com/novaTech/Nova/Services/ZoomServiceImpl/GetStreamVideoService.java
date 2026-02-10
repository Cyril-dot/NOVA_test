package com.novaTech.Nova.Services.ZoomServiceImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * GetStream Video Service
 * Handles user token generation and video call URL creation
 */
@Service
@Slf4j
public class GetStreamVideoService {

    @Value("${getstream.api.key}")
    private String apiKey;

    @Value("${getstream.api.secret}")
    private String apiSecret;

    /**
     * Generate a user token for GetStream Video
     * This allows users to join calls without pre-creating accounts
     * 
     * @param userId The user's UUID
     * @param userName The user's display name (optional)
     * @return JWT token for GetStream authentication
     */
    public String generateUserToken(UUID userId, String userName) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (24 * 60 * 60 * 1000); // 24 hours from now

        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId.toString());
        if (userName != null && !userName.isEmpty()) {
            claims.put("name", userName);
        }

        return Jwts.builder()
                .setIssuer("ampere")
                .setSubject("user/" + userId)
                .claim("user_id", userId.toString())
                .claim("api_key", apiKey)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .signWith(SignatureAlgorithm.HS256, apiSecret.getBytes())
                .compact();
    }

    /**
     * Generate the proper GetStream video call URL
     * This should point to YOUR frontend application, not GetStream's dashboard
     * 
     * @param callId The unique call identifier
     * @return URL to your frontend video call page
     */
    public String generateVideoCallUrl(String callId) {
        // This should be YOUR frontend URL where you've integrated GetStream Video SDK
        // Example: https://yourapp.com/call/{callId}
        return String.format("https://yourapp.com/call/%s", callId);
    }

    /**
     * Generate call data for frontend
     * Frontend will use this to initialize the GetStream Video call
     * 
     * @param userId User's UUID
     * @param userName User's display name
     * @param callId Call identifier
     * @return Map with all data needed for the frontend
     */
    public Map<String, String> generateCallData(UUID userId, String userName, String callId) {
        Map<String, String> callData = new HashMap<>();
        
        callData.put("apiKey", apiKey);
        callData.put("userId", userId.toString());
        callData.put("userName", userName);
        callData.put("callId", callId);
        callData.put("userToken", generateUserToken(userId, userName));
        callData.put("callType", "default");
        
        return callData;
    }
}