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

        public JwtAuthenticationFilter(TokenService tokenService, UserRepo userRepo) {
            this.tokenService = tokenService;
            this.userRepo = userRepo;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String requestPath = request.getRequestURI();

            // Skip logging for /error to avoid noise
            if (!requestPath.equals("/error")) {
                log.debug("🔍 [JWT FILTER] {} {}", request.getMethod(), requestPath);
            }

            // Skip public endpoints
            if (requestPath.startsWith("/api/v1/ai/") ||
                    requestPath.startsWith("/api/v1/external/") ||
                    requestPath.startsWith("/api/auth/register") ||
                    requestPath.startsWith("/api/auth/login") ||
                    requestPath.startsWith("/api/auth/login-mfa") ||
                    requestPath.startsWith("/api/auth/verify-mfa") ||
                    requestPath.startsWith("/api/auth/generate-mfa") ||
                    requestPath.startsWith("/api/auth/view-mfa") ||
                    requestPath.startsWith("/api/auth/mfa-code") ||
                    requestPath.startsWith("/api/test/") ||
                    requestPath.startsWith("/oauth2/") ||
                    requestPath.equals("/error") ||
                    requestPath.startsWith("/actuator/")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Skip if already authenticated (prevents double-processing)
            var existingAuth = SecurityContextHolder.getContext().getAuthentication();
            if (existingAuth != null &&
                    existingAuth.isAuthenticated() &&
                    !(existingAuth instanceof AnonymousAuthenticationToken)) {
                log.trace("🔄 [JWT FILTER] Already authenticated - skipping");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String header = request.getHeader("Authorization");

                if (header == null || !header.startsWith("Bearer ")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String token = header.substring(7);

                if (!tokenService.validateAccessToken(token)) {
                    log.warn("❌ [JWT FILTER] Invalid token for: {}", requestPath);
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = tokenService.getEmailFromAccessToken(token);
                User user = userRepo.findByEmail(email).orElse(null);

                if (user == null) {
                    log.warn("❌ [JWT FILTER] User not found: {}", email);
                    filterChain.doFilter(request, response);
                    return;
                }

                var userDetails = new UserPrincipal(user);

                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("✅ [JWT FILTER] Authenticated: {}", email);

            } catch (Exception e) {
                log.error("💥 [JWT FILTER] Error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }

            filterChain.doFilter(request, response);
        }
    }
}
