package com.novaTech.Nova.Security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    // ✅ Thread-safe in-memory store — keyed by state param
    private final Map<String, OAuth2AuthorizationRequest> store = new ConcurrentHashMap<>();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            log.warn("⚠️ [OAUTH2] No state param in request");
            return null;
        }

        OAuth2AuthorizationRequest authRequest = store.get(state);
        if (authRequest == null) {
            log.warn("⚠️ [OAUTH2] No authorization request found for state: {}", state);
        } else {
            log.debug("✅ [OAUTH2] Found authorization request for state: {}", state);
        }

        return authRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            String state = request.getParameter("state");
            if (state != null) {
                store.remove(state);
                log.debug("🗑️ [OAUTH2] Removed authorization request for state: {}", state);
            }
            return;
        }

        String state = authorizationRequest.getState();
        store.put(state, authorizationRequest);
        log.info("✅ [OAUTH2] Saved authorization request for state: {}", state);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        String state = request.getParameter("state");
        if (state == null) {
            log.warn("⚠️ [OAUTH2] No state param — cannot remove authorization request");
            return null;
        }

        OAuth2AuthorizationRequest authRequest = store.remove(state);
        if (authRequest != null) {
            log.debug("✅ [OAUTH2] Removed and returned authorization request for state: {}", state);
        }
        return authRequest;
    }
}