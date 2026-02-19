package com.novaTech.Nova.Services.ZoomServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebRTCWebSocketConfig implements WebSocketConfigurer {

    private final WebRTCSignalingHandler webRTCSignalingHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(webRTCSignalingHandler, "/ws/webrtc")
                .setAllowedOrigins("*");

        log.info("✅ WebSocket handler registered at /ws/webrtc");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}