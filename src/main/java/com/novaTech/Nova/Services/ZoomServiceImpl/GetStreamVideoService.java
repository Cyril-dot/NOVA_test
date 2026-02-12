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
     */
    public String generateUserToken(UUID userId, String userName) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (24 * 60 * 60 * 1000);

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
     * Generate call data for frontend
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