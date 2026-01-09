package com.yamoyo.be.oauth.domain.security.refreshtoken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Refresh Token Repository
 *
 * Role:
 * - Refresh Token에 대한 데이터베이스 접근 계층
 * - JpaRepository를 상속받아 기본 CRUD 기능 제공
 *
 * Complexity/Rationale:
 * - userId로 조회: 로그아웃 시 해당 사용자의 Refresh Token 삭제
 * - token으로 조회: Refresh Token 갱신 시 DB에 저장된 토큰과 비교
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * userId로 Refresh Token 조회
     *
     * Role:
     * - 로그아웃 시 해당 사용자의 Refresh Token 삭제
     * - 중복 로그인 시 기존 Refresh Token 갱신
     *
     * @param userId 사용자 ID
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByUserId(Long userId);

    /**
     * Refresh Token 문자열로 조회
     *
     * Role:
     * - Access Token 재발급 요청 시 Refresh Token 유효성 검증
     * - DB에 저장된 Refresh Token과 일치하는지 확인
     *
     * @param token Refresh Token 문자열
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * userId로 Refresh Token 삭제
     *
     * Role:
     * - 로그아웃 시 해당 사용자의 Refresh Token 삭제
     * - Refresh Token 무효화
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(Long userId);
}
