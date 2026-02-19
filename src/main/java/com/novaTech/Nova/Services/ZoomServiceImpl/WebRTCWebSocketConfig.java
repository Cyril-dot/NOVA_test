package com.novaTech.Nova.Services.ZoomServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * WebSocket Configuration
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * NOTE ON ARCHITECTURE (Daily.co integration):
 * ─────────────────────────────────────────────
 * Since the frontend now uses Daily.co (daily-js SDK) for all video, audio,
 * and screen-sharing, the full WebRTC peer-to-peer signaling stack
 * (SDP offer/answer, ICE candidates) is handled entirely by Daily's
 * infrastructure — NOT by WebRTCSignalingHandler.
 *
 * This WebSocket endpoint (/ws/webrtc) is now used only for:
 *   • Participant presence tracking  (JOIN / LEAVE events → DB)
 *   • Meeting lifecycle events       (END / RESTART broadcasts)
 *   • Optional: server-side chat relay if you ever move off Daily's
 *     sendAppMessage and want persistent chat history
 *
 * If you want to remove WebRTC signaling entirely and rely 100% on Daily,
 * you can delete WebRTCSignalingHandler and this config class safely —
 * Daily handles all of that server-side for you.
 *
 * Endpoints registered:
 *   /ws/webrtc  →  WebRTCSignalingHandler  (presence + lifecycle events)
 * ─────────────────────────────────────────────────────────────────────────────
 */
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
                /*
                 * CORS: Replace "*" with your actual frontend origin(s) in production.
                 *
                 * Examples:
                 *   .setAllowedOrigins("https://your-app.com", "https://www.your-app.com")
                 *
                 * For local dev with a Vite/Vue frontend on port 5173:
                 *   .setAllowedOrigins("http://localhost:5173")
                 *
                 * Using "*" is fine for development but is a security risk in production
                 * because any website can open a WebSocket connection to your server.
                 */
                .setAllowedOrigins("*");

        log.info("✅ WebSocket handler registered at /ws/webrtc");
    }
}