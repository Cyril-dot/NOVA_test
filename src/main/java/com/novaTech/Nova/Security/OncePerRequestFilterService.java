package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OncePerRequestFilterService {

    private final TokenService tokenService;
    private final UserRepo userRepo;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenService, userRepo);
    }

    @Slf4j
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final TokenService tokenService;
        private final UserRepo userRepo;

        // List of public auth endpoints that don't need JWT
        private static final List<String> PUBLIC_AUTH_ENDPOINTS = Arrays.asList(
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/login-mfa",
                "/api/auth/verify-mfa",
                "/api/auth/generate-mfa",
                "/api/auth/view-mfa",
                "/api/auth/mfa-code",
                "/api/auth/refresh"
        );

        public JwtAuthenticationFilter(TokenService tokenService, UserRepo userRepo) {
            this.tokenService = tokenService;
            this.userRepo = userRepo;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();

            // Check if it's a public auth endpoint
            if (PUBLIC_AUTH_ENDPOINTS.stream().anyMatch(path::startsWith)) {
                return true;
            }

            // Check other public paths
            return path.startsWith("/login")
                    || path.startsWith("/oauth2")
                    || path.startsWith("/error")
                    || path.startsWith("/api/oauth2/")
                    || path.startsWith("/api/v1/ai/")
                    || path.startsWith("/api/v1/external/")
                    || path.startsWith("/api/test/")
                    || path.startsWith("/actuator/")
                    || path.startsWith("/ws/")
                    || path.startsWith("/ws-meeting/")
                    || path.equals("/favicon.ico")
                    || path.equals("/api/meetings/join/guest")
                    || path.startsWith("/api/meetings/validate/")
                    || (path.matches("/api/meetings/[A-Z0-9-]+") && request.getMethod().equals("GET"))
                    || path.startsWith("/.well-known/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String requestPath = request.getRequestURI();
            log.debug("🔍 [JWT FILTER] Processing: {} {}", request.getMethod(), requestPath);

            // Skip if already authenticated
            var existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null &&
                    existingAuth.isAuthenticated() &&
                    !(existingAuth instanceof AnonymousAuthenticationToken)) {
                log.trace("🔄 [JWT FILTER] Already authenticated - skipping");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // Extract Authorization header
                String header = request.getHeader("Authorization");

                log.debug("📥 [JWT FILTER] Authorization header: {}",
                        header != null ? "Bearer " + header.substring(7, Math.min(27, header.length())) + "..." : "null");

                // If no header or doesn't start with Bearer, skip
                if (header == null || !header.startsWith("Bearer ")) {
                    log.debug("⚠️ [JWT FILTER] No Bearer token found for {} {}, continuing without authentication",
                            request.getMethod(), requestPath);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extract encrypted token (everything after "Bearer ")
                String encryptedToken = header.substring(7);
                log.debug("🔐 [JWT FILTER] Encrypted token extracted (length: {})", encryptedToken.length());
                log.debug("🔐 [JWT FILTER] Token preview: {}...", encryptedToken.substring(0, Math.min(50, encryptedToken.length())));

                // Extract email from token (this internally decrypts and validates)
                String email;
                try {
                    email = tokenService.getEmailFromAccessToken(encryptedToken);
                    log.debug("✅ [JWT FILTER] Email extracted from token: {}", email);
                } catch (Exception e) {
                    log.warn("❌ [JWT FILTER] Failed to extract email from token for {} {}: {}",
                            request.getMethod(), requestPath, e.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

                // Find user by email
                User user = userRepo.findByEmail(email).orElse(null);

                if (user == null) {
                    log.warn("❌ [JWT FILTER] User not found in database: {}", email);
                    filterChain.doFilter(request, response);
                    return;
                }

                log.debug("✅ [JWT FILTER] User found: {} (ID: {}, Role: {})",
                        user.getEmail(), user.getId(), user.getRole());

                // Create UserPrincipal and set authentication
                var userDetails = new UserPrincipal(user);
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("✅ [JWT FILTER] Authentication successful for: {} on {} {}",
                        email, request.getMethod(), requestPath);

            } catch (Exception e) {
                log.error("💥 [JWT FILTER] Unexpected error during authentication for {} {}: {}",
                        request.getMethod(), requestPath, e.getMessage(), e);
                SecurityContextHolder.clearContext();
            }

            filterChain.doFilter(request, response);
        }
    }
}