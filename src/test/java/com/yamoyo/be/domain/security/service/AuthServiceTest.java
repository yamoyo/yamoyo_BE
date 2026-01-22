package com.yamoyo.be.domain.security.service;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.jwt.JwtTokenProvider;
import com.yamoyo.be.domain.security.refreshtoken.RefreshToken;
import com.yamoyo.be.domain.security.refreshtoken.RefreshTokenRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트
 *
 * 테스트 내용:
 * 1. refresh() - Refresh Token으로 Access Token 재발급 테스트
 * 2. logout() - 로그아웃 처리 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String PROVIDER = "google";
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final Long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7일

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("refresh() - 유효한 RefreshToken으로 AccessToken 재발급 성공")
    void refresh_ValidRefreshToken_Success() {
        // given
        RefreshToken storedRefreshToken = RefreshToken.create(USER_ID, OLD_REFRESH_TOKEN, LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(storedRefreshToken, "id", 1L);

        User user = User.create(USER_EMAIL, "Test User", null);
        ReflectionTestUtils.setField(user, "id", USER_ID);

        JwtTokenClaims claims = new JwtTokenClaims(USER_ID, USER_EMAIL, PROVIDER);
        JwtTokenDto newTokens = new JwtTokenDto("Bearer", NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, 600000L);

        given(refreshTokenRepository.findByToken(OLD_REFRESH_TOKEN)).willReturn(Optional.of(storedRefreshToken));
        given(jwtTokenProvider.parseClaimsFromExpiredToken(OLD_REFRESH_TOKEN)).willReturn(claims);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateToken(USER_ID, USER_EMAIL, PROVIDER)).willReturn(newTokens);

        // when
        JwtTokenDto result = authService.refresh(OLD_REFRESH_TOKEN);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(result.grantType()).isEqualTo("Bearer");

        // verify DB에서 새로운 RefreshToken으로 업데이트되었는지 확인
        assertThat(storedRefreshToken.getToken()).isEqualTo(NEW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh() - DB에 없는 RefreshToken으로 재발급 시도 시 예외 발생")
    void refresh_InvalidRefreshToken_ThrowsException() {
        // given
        String invalidToken = "invalid-refresh-token";
        given(refreshTokenRepository.findByToken(invalidToken)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh(invalidToken))
                .isInstanceOf(YamoyoException.class)
                .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        verify(jwtTokenProvider, never()).parseClaimsFromExpiredToken(anyString());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("refresh() - 만료된 RefreshToken으로 재발급 시도 시 예외 발생")
    void refresh_ExpiredRefreshToken_ThrowsException() {
        // given
        RefreshToken expiredRefreshToken = RefreshToken.create(USER_ID, OLD_REFRESH_TOKEN, LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(expiredRefreshToken, "id", 1L);

        given(refreshTokenRepository.findByToken(OLD_REFRESH_TOKEN)).willReturn(Optional.of(expiredRefreshToken));

        // when & then
        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(YamoyoException.class)
                .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));

        // 만료된 토큰은 DB에서 삭제되어야 함
        verify(refreshTokenRepository).delete(expiredRefreshToken);
        verify(jwtTokenProvider, never()).parseClaimsFromExpiredToken(anyString());
    }

    @Test
    @DisplayName("refresh() - 사용자를 찾을 수 없는 경우 예외 발생")
    void refresh_UserNotFound_ThrowsException() {
        // given
        RefreshToken storedRefreshToken = RefreshToken.create(USER_ID, OLD_REFRESH_TOKEN, LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(storedRefreshToken, "id", 1L);

        JwtTokenClaims claims = new JwtTokenClaims(USER_ID, USER_EMAIL, PROVIDER);

        given(refreshTokenRepository.findByToken(OLD_REFRESH_TOKEN)).willReturn(Optional.of(storedRefreshToken));
        given(jwtTokenProvider.parseClaimsFromExpiredToken(OLD_REFRESH_TOKEN)).willReturn(claims);
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(YamoyoException.class)
                .satisfies(e -> assertThat(((YamoyoException) e).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("refresh() - RefreshToken이 DB에 저장되어 있는지 확인")
    void refresh_RefreshTokenStoredInDB() {
        // given
        RefreshToken storedRefreshToken = RefreshToken.create(USER_ID, OLD_REFRESH_TOKEN, LocalDateTime.now().plusDays(7));
        ReflectionTestUtils.setField(storedRefreshToken, "id", 1L);

        User user = User.create(USER_EMAIL, "Test User", null);
        ReflectionTestUtils.setField(user, "id", USER_ID);

        JwtTokenClaims claims = new JwtTokenClaims(USER_ID, USER_EMAIL, PROVIDER);
        JwtTokenDto newTokens = new JwtTokenDto("Bearer", NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, 600000L);

        given(refreshTokenRepository.findByToken(OLD_REFRESH_TOKEN)).willReturn(Optional.of(storedRefreshToken));
        given(jwtTokenProvider.parseClaimsFromExpiredToken(OLD_REFRESH_TOKEN)).willReturn(claims);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateToken(USER_ID, USER_EMAIL, PROVIDER)).willReturn(newTokens);

        // when
        authService.refresh(OLD_REFRESH_TOKEN);

        // then - RefreshToken이 DB에서 조회되었는지 확인
        verify(refreshTokenRepository).findByToken(OLD_REFRESH_TOKEN);

        // RefreshToken이 새로운 토큰으로 업데이트되었는지 확인
        assertThat(storedRefreshToken.getToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(storedRefreshToken.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("logout() - 정상 로그아웃 시 RefreshToken 삭제")
    void logout_Success() {
        // when
        authService.logout(USER_ID);

        // then
        verify(refreshTokenRepository).deleteByUserId(USER_ID);
    }

    @Test
    @DisplayName("logout() - 이미 로그아웃된 사용자도 정상 처리 (멱등성)")
    void logout_AlreadyLoggedOut_Success() {
        // given - 이미 RefreshToken이 없는 상태

        // when - 로그아웃 호출해도 예외 발생하지 않음
        authService.logout(USER_ID);

        // then - deleteByUserId 호출됨 (없어도 예외 발생 안함)
        verify(refreshTokenRepository).deleteByUserId(USER_ID);
    }
}
