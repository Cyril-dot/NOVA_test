package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final TokenService tokenService;
    private final UserRepo userRepo;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(TokenService tokenService,
                          UserRepo userRepo,
                          CustomOAuth2UserService customOAuth2UserService) {
        this.tokenService = tokenService;
        this.userRepo = userRepo;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/oauth2/**",
                                "/api/v1/external/**",
                                "/api/v1/ai/**",
                                "/error",
                                "/actuator/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/chat/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("🚫 [AUTH] Unauthorized: {} {}", request.getMethod(), request.getRequestURI());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenService, userRepo);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var oAuth2User = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");

            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

            String jwt = tokenService.generateAccessToken(user);
            String refreshToken = tokenService.generateRefreshToken(user).getToken();

            log.info("✅ [OAUTH2] User {} logged in", user.getEmail());

            response.setContentType("application/json");
            response.getWriter().write("{\"access_token\":\"" + jwt + "\", \"refreshToken\":\"" + refreshToken + "\"}");
            response.getWriter().flush();
        };
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
                    requestPath.startsWith("/api/auth/") ||
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

                var userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();

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