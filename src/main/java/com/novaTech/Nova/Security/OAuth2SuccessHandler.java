package com.novaTech.Nova.Security;

import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Security.entity.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final UserRepo userRepo;

    @Value("${app.frontend-url:https://novaspace-3xjlmad36-cyril-dots-projects.vercel.app}")
    private String frontendUrl;

    public OAuth2SuccessHandler(TokenService tokenService, UserRepo userRepo) {
        this.tokenService = tokenService;
        this.userRepo = userRepo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");

            log.info("✅ OAuth2 Authentication Success - Email: {}", email);

            // Get user from database
            User user = userRepo.findByEmail(email).orElse(null);

            if (user == null) {
                log.error("❌ User not found after OAuth2 authentication - Email: {}", email);
                String errorUrl = frontendUrl + "/login?error=user_not_found";
                response.sendRedirect(errorUrl);
                return;
            }

            log.info("👤 User found in database - ID: {}, Email: {}", user.getId(), user.getEmail());

            // Generate encrypted access token using TokenService
            String accessToken = tokenService.generateAccessToken(user);
            log.info("🔐 Access token generated (length: {})", accessToken.length());

            // Generate encrypted refresh token using TokenService
            RefreshToken refreshTokenEntity = tokenService.generateRefreshToken(user);
            String refreshToken = refreshTokenEntity.getToken();
            log.info("🔄 Refresh token generated (length: {})", refreshToken.length());

            // Redirect to frontend dashboard with tokens
            String targetUrl = buildRedirectUrl(accessToken, refreshToken);

            log.info("🚀 Redirecting to: {}/dashboard", frontendUrl);
            response.sendRedirect(targetUrl);

        } catch (Exception ex) {
            log.error("❌ OAuth2 Success Handler Error: {}", ex.getMessage(), ex);
            String errorUrl = frontendUrl + "/login?error=oauth_failed";
            try {
                response.sendRedirect(errorUrl);
            } catch (IOException ioEx) {
                log.error("❌ Failed to redirect on error: {}", ioEx.getMessage());
            }
        }
    }

    /**
     * Build redirect URL with tokens
     */
    private String buildRedirectUrl(String accessToken, String refreshToken) {
        try {
            // URL encode tokens to handle special characters
            String encodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
            String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            String targetUrl = String.format(
                    "%s/dashboard?accessToken=%s&refreshToken=%s",
                    frontendUrl,
                    encodedAccessToken,
                    encodedRefreshToken
            );

            log.debug("Redirect URL built successfully");
            return targetUrl;

        } catch (Exception ex) {
            log.error("❌ Failed to build redirect URL: {}", ex.getMessage());
            return frontendUrl + "/login?error=redirect_failed";
        }
    }
}