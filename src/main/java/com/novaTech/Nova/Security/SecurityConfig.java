package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final TokenService tokenService;
    private final UserRepo userRepo;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OncePerRequestFilterService oncePerRequestFilterService;

    public SecurityConfig(TokenService tokenService,
                          UserRepo userRepo,
                          CustomOAuth2UserService customOAuth2UserService,
                          OncePerRequestFilterService oncePerRequestFilterService) {
        this.tokenService = tokenService;
        this.userRepo = userRepo;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oncePerRequestFilterService = oncePerRequestFilterService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())

                // ✅ IF_REQUIRED lets Spring briefly create a session for OAuth2 state param
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // Auth endpoints
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/login-mfa",
                                "/api/auth/verify-mfa",
                                "/api/auth/generate-mfa",
                                "/api/auth/view-mfa",
                                "/api/auth/mfa-code",
                                // ✅ Spring Security OAuth2 internal paths
                                "/oauth2/**",
                                "/login",
                                "/login/**",
                                // ✅ Your OAuthController trigger endpoints
                                "/api/oauth2/**",
                                // Other public
                                "/api/test/**",
                                "/api/v1/external/**",
                                "/api/v1/ai/**",
                                "/error",
                                "/error/**",
                                "/actuator/**",
                                "/ws-meeting/**",
                                "/ws/**",
                                "/api/meetings/**",
                                "/favicon.ico",  // ✅ FIXED: removed /** wildcard
                                "/assets/**",     // ✅ FIXED: also fixed typo "assests" → "assets"
                                "/.well-known/**"
                        ).permitAll()

                        .requestMatchers("/api/v1/chat/**").authenticated()
                        .anyRequest().authenticated()
                )

                // ✅ Returns JSON 401 instead of redirecting to /login
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("🚫 [AUTH] Unauthorized: {} {}", request.getMethod(), request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Unauthorized\", \"message\": \""
                                            + authException.getMessage() + "\"}"
                            );
                            response.getWriter().flush();
                        })
                )

                .addFilterBefore(
                        oncePerRequestFilterService.jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                // ✅ Store OAuth2 state in cookie instead of session
                                // Fixes: authorization_request_not_found / invalid session id
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                        .failureHandler(oAuth2AuthenticationFailureHandler())
                );

        return http.build();
    }

    // ✅ Replace the old cookie-based bean with this
    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new InMemoryOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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

            log.info("✅ [OAUTH2] User {} logged in successfully", user.getEmail());

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"access_token\":\"" + jwt + "\", \"refresh_token\":\"" + refreshToken + "\"}"
            );
            response.getWriter().flush();
        };
    }

    @Bean
    public AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return (request, response, exception) -> {
            log.error("❌ [OAUTH2] Authentication failed: {}", exception.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"OAuth2 authentication failed\", \"message\": \""
                            + exception.getMessage() + "\"}"
            );
            response.getWriter().flush();
        };
    }
}