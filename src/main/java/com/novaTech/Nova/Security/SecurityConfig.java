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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final TokenService tokenService;
    private final UserRepo userRepo;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OncePerRequestFilterService oncePerRequestFilterService;  // ✅ Renamed for clarity

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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/login-mfa",
                                "/api/auth/verify-mfa",
                                "/api/auth/generate-mfa",
                                "/api/auth/view-mfa",
                                "/api/auth/mfa-code",
                                "api/test/**",
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
                .addFilterBefore(
                        oncePerRequestFilterService.jwtAuthenticationFilter(),  // ✅ Get the actual filter bean
                        UsernamePasswordAuthenticationFilter.class
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                );

        return http.build();
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

            log.info("✅ [OAUTH2] User {} logged in", user.getEmail());

            response.setContentType("application/json");
            response.getWriter().write("{\"access_token\":\"" + jwt + "\", \"refreshToken\":\"" + refreshToken + "\"}");
            response.getWriter().flush();
        };
    }
}