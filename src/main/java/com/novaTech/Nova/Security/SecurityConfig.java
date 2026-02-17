package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Security.RateLimitingConfigs.RateLimitFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final TokenService tokenService;
    private final UserRepo userRepo;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OncePerRequestFilterService oncePerRequestFilterService;
    private final RateLimitFilter rateLimitFilter;

    @Value("${app.frontend-url:https://novaspace-3xjlmad36-cyril-dots-projects.vercel.app}")
    private String frontendUrl;

    public SecurityConfig(TokenService tokenService,
                          UserRepo userRepo,
                          CustomOAuth2UserService customOAuth2UserService,
                          OncePerRequestFilterService oncePerRequestFilterService,
                          RateLimitFilter rateLimitFilter) {
        this.tokenService = tokenService;
        this.userRepo = userRepo;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oncePerRequestFilterService = oncePerRequestFilterService;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/login-mfa",
                                "/api/auth/verify-mfa",
                                "/api/auth/generate-mfa",
                                "/api/auth/view-mfa",
                                "/api/auth/mfa-code",
                                "/oauth2/**",
                                "/login",
                                "/login/**",
                                "/api/oauth2/**",
                                "/api/test/**",
                                "/api/auth/refresh",
                                "/error",
                                "/error/**",
                                "/actuator/**",
                                "/ws-meeting/**",
                                "/ws/**",
                                "/favicon.ico",
                                "/assets/**",
                                "/.well-known/**"
                        ).permitAll()
                        .requestMatchers("/api/meetings/join/guest").permitAll()
                        .requestMatchers("/api/meetings/validate/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/meetings/{meetingCode}").permitAll()
                        .requestMatchers("/api/meetings/**").authenticated()
                        .requestMatchers("/api/v1/chat/**").authenticated()
                        .anyRequest().authenticated()
                )

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

                .addFilterBefore((Filter) rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        oncePerRequestFilterService.jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )

                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
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
            try {
                var oAuth2User = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                String email = oAuth2User.getAttribute("email");

                log.info("🔥 OAuth2 Success Handler - Email: {}", email);

                User user = userRepo.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

                // Generate encrypted tokens
                String encryptedAccessToken = tokenService.generateAccessToken(user);
                var refreshTokenEntity = tokenService.generateRefreshToken(user);
                String encryptedRefreshToken = refreshTokenEntity.getToken();

                log.info("✅ Tokens generated - Access Token length: {}, Refresh Token length: {}",
                        encryptedAccessToken.length(), encryptedRefreshToken.length());

                // URL encode tokens
                String encodedAccessToken = URLEncoder.encode(encryptedAccessToken, StandardCharsets.UTF_8);
                String encodedRefreshToken = URLEncoder.encode(encryptedRefreshToken, StandardCharsets.UTF_8);

                // Build redirect URL
                String redirectUrl = String.format(
                        "%s/dashboard?accessToken=%s&refreshToken=%s",
                        frontendUrl,
                        encodedAccessToken,
                        encodedRefreshToken
                );

                log.info("🚀 Redirecting to: {}/dashboard", frontendUrl);
                log.info("✅ [OAUTH2] User {} logged in successfully", user.getEmail());

                response.sendRedirect(redirectUrl);

            } catch (Exception ex) {
                log.error("❌ OAuth2 Success Handler Error: {}", ex.getMessage(), ex);
                try {
                    response.sendRedirect(frontendUrl + "/login?error=oauth_failed");
                } catch (Exception redirectEx) {
                    log.error("Failed to send error redirect: {}", redirectEx.getMessage());
                }
            }
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