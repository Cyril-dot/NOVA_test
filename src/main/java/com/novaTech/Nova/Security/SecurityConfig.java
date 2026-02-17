package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Security.RateLimitingConfigs.RateLimitFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final TokenService tokenService;
    private final UserRepo userRepo;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OncePerRequestFilterService oncePerRequestFilterService;
    private final RateLimitFilter rateLimitFilter;

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
            var oAuth2User = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");

            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

            String encryptedAccessToken = tokenService.generateAccessToken(user);
            String encryptedRefreshToken = tokenService.generateRefreshToken(user).getToken();

            log.info("✅ [OAUTH2] User {} logged in successfully", user.getEmail());

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"access_token\":\"" + encryptedAccessToken +
                            "\", \"refresh_token\":\"" + encryptedRefreshToken + "\"}"
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