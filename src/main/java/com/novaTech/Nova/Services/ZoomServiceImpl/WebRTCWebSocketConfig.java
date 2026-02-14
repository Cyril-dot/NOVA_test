package com.novaTech.Nova.Services.ZoomServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket Configuration for WebRTC Signaling
 * 
 * Registers the WebRTC signaling handler at /ws/webrtc
 * This is separate from your existing WebSocket config
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebRTCWebSocketConfig implements WebSocketConfigurer {

    private final WebRTCSignalingHandler webRTCSignalingHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webRTCSignalingHandler, "/ws/webrtc")
                .setAllowedOrigins("*"); // Configure CORS as needed
    }
}