package com.yamoyo.be.domain.security.oauth.handler;

import com.yamoyo.be.domain.security.jwt.JwtProperties;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.jwt.JwtTokenProvider;
import com.yamoyo.be.domain.security.oauth.CustomOAuth2User;
import com.yamoyo.be.domain.security.oauth.CookieProperties;
import com.yamoyo.be.domain.security.refreshtoken.RefreshToken;
import com.yamoyo.be.domain.security.refreshtoken.RefreshTokenRepository;
import com.yamoyo.be.domain.user.entity.OnboardingStatus;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OAuth2 Authentication Success Handler
 *
 * Role:
 * - OAuth2 로그인 성공 시 JWT 토큰을 발급하고 프론트엔드로 리다이렉트
 * - email 기반으로 사용자 조회 후 토큰 발급
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @Value("${app.front.base-url}")
    private String frontBaseUrl;

    @Value("${app.front.paths.home}")
    private String homePath;

    @Value("${app.front.paths.onboarding.terms}")
    private String onboardingTermsPath;

    @Value("${app.front.paths.onboarding.profile}")
    private String onboardingProfilePath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        CustomOAuth2User oAuth2User = (CustomOAuth2User) oAuth2Token.getPrincipal();

        String provider = oAuth2Token.getAuthorizedClientRegistrationId();
        String email = oAuth2User.getAttribute("email");

        log.info("OAuth2 로그인 성공 - Provider: {}, Email: {}", provider, email);

        // email로 User 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "OAuth2 로그인 성공했으나 DB에 사용자가 없습니다. Email: " + email));

        // JWT 토큰 생성
        JwtTokenDto jwtToken = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                provider
        );

        log.info("JWT 토큰 발급 완료 - UserId: {}, Email: {}", user.getId(), user.getEmail());

        // Refresh Token DB 저장
        saveRefreshToken(user.getId(), jwtToken.refreshToken(), jwtProperties.refreshTokenExpiration());

        // Refresh Token을 HttpOnly Cookie에 설정
        addRefreshTokenCookie(response, jwtToken.refreshToken(), jwtProperties.refreshTokenExpiration());

        // OnboardingStatus에 따른 리다이렉트
        String redirectUri = determineRedirectUri(oAuth2User);
        log.info("리다이렉트 - OnboardingStatus: {}, URI: {}", oAuth2User.getOnboardingStatus(), redirectUri);
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String determineRedirectUri(CustomOAuth2User oAuth2User) {
        OnboardingStatus status = oAuth2User.getOnboardingStatus();

        return switch (status) {
            case TERMS_PENDING -> frontBaseUrl + onboardingTermsPath;
            case PROFILE_PENDING -> frontBaseUrl + onboardingProfilePath;
            case COMPLETED -> frontBaseUrl + homePath;
        };
    }

    private void saveRefreshToken(Long userId, String refreshToken, Long refreshTokenExpiration) {
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existingToken -> {
                            existingToken.updateToken(refreshToken, expiryDate);
                            refreshTokenRepository.save(existingToken);
                        },
                        () -> refreshTokenRepository.save(RefreshToken.create(userId, refreshToken, expiryDate))
                );

        log.debug("Refresh Token 저장 완료 - UserId: {}, ExpiryDate: {}", userId, expiryDate);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, Long refreshTokenExpiration) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieProperties.secure());
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        response.addCookie(cookie);
    }
}
