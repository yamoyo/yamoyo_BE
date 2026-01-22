package com.yamoyo.be.domain.security.refreshtoken;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RefreshToken Repository 테스트
 *
 * 테스트 내용:
 * 1. RefreshToken DB 저장 테스트
 * 2. userId로 RefreshToken 조회 테스트
 * 3. token 문자열로 RefreshToken 조회 테스트
 * 4. RefreshToken 삭제 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("RefreshToken 저장 - 성공")
    void save_RefreshToken_Success() {
        // given
        Long userId = 1L;
        String token = "test-refresh-token";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();

        // when
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        // then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getUserId()).isEqualTo(userId);
        assertThat(savedToken.getToken()).isEqualTo(token);
        assertThat(savedToken.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(savedToken.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("userId로 RefreshToken 조회 - 성공")
    void findByUserId_Success() {
        // given
        Long userId = 1L;
        String token = "test-refresh-token";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();
        refreshTokenRepository.save(refreshToken);

        // when
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserId(userId);

        // then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getUserId()).isEqualTo(userId);
        assertThat(foundToken.get().getToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("userId로 RefreshToken 조회 - 존재하지 않는 경우")
    void findByUserId_NotFound() {
        // given
        Long nonExistentUserId = 999L;

        // when
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserId(nonExistentUserId);

        // then
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("token 문자열로 RefreshToken 조회 - 성공")
    void findByToken_Success() {
        // given
        Long userId = 1L;
        String token = "test-refresh-token-string";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();
        refreshTokenRepository.save(refreshToken);

        // when
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByToken(token);

        // then
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getToken()).isEqualTo(token);
        assertThat(foundToken.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("token 문자열로 RefreshToken 조회 - 존재하지 않는 경우")
    void findByToken_NotFound() {
        // given
        String nonExistentToken = "non-existent-token";

        // when
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByToken(nonExistentToken);

        // then
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("userId로 RefreshToken 삭제 - 성공")
    void deleteByUserId_Success() {
        // given
        Long userId = 1L;
        String token = "test-refresh-token";
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiryDate(expiryDate)
                .build();
        refreshTokenRepository.save(refreshToken);

        // when
        refreshTokenRepository.deleteByUserId(userId);

        // then
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserId(userId);
        assertThat(foundToken).isEmpty();
    }

    @Test
    @DisplayName("RefreshToken 업데이트 - 성공")
    void updateToken_Success() {
        // given
        Long userId = 1L;
        String oldToken = "old-refresh-token";
        LocalDateTime oldExpiryDate = LocalDateTime.now().plusDays(7);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(oldToken)
                .expiryDate(oldExpiryDate)
                .build();
        refreshTokenRepository.save(refreshToken);

        // when
        String newToken = "new-refresh-token";
        LocalDateTime newExpiryDate = LocalDateTime.now().plusDays(14);

        RefreshToken foundToken = refreshTokenRepository.findByUserId(userId).orElseThrow();
        foundToken.updateToken(newToken, newExpiryDate);
        refreshTokenRepository.flush();

        // then
        RefreshToken updatedToken = refreshTokenRepository.findByUserId(userId).orElseThrow();
        assertThat(updatedToken.getToken()).isEqualTo(newToken);
        assertThat(updatedToken.getExpiryDate()).isEqualTo(newExpiryDate);
    }

    @Test
    @DisplayName("RefreshToken 만료 여부 확인 - 만료된 토큰")
    void isExpired_True() {
        // given
        RefreshToken expiredToken = RefreshToken.builder()
                .userId(1L)
                .token("expired-token")
                .expiryDate(LocalDateTime.now().minusDays(1))
                .build();

        // when & then
        assertThat(expiredToken.isExpired()).isTrue();
    }

    @Test
    @DisplayName("RefreshToken 만료 여부 확인 - 유효한 토큰")
    void isExpired_False() {
        // given
        RefreshToken validToken = RefreshToken.builder()
                .userId(1L)
                .token("valid-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        // when & then
        assertThat(validToken.isExpired()).isFalse();
    }
}
