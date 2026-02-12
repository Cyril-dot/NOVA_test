package com.novaTech.Nova.Services.Configs;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenService tokenService;
    private final UserRepo userRepo;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for server-to-client messages
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint - NO SockJS
        registry.addEndpoint("/ws-meeting")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://poikiloblastic-leeanne-gazeless.ngrok-free.dev",
                        "https://cyril-dot.github.io"
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            // Validate JWT token
                            if (tokenService.validateAccessToken(token)) {
                                // Extract email from token
                                String email = tokenService.getEmailFromAccessToken(token);

                                // Look up user to get UUID
                                User user = userRepo.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException("User not found: " + email));

                                // Create authentication object with UUID as principal name
                                Authentication auth = new UsernamePasswordAuthenticationToken(
                                        user.getId().toString(),  // UUID as string
                                        null,
                                        null
                                );
                                accessor.setUser(auth);
                                log.info("✅ WebSocket authenticated: userId={}, email={}", user.getId(), email);
                            } else {
                                log.warn("⚠️ Invalid WebSocket token");
                            }
                        } catch (Exception e) {
                            log.error("❌ WebSocket authentication failed: {}", e.getMessage());
                        }
                    } else {
                        log.warn("⚠️ No Authorization header in WebSocket connection");
                    }
                }

                return message;
            }
        });
    }
}