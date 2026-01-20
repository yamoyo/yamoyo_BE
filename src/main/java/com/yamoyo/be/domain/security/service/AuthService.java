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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Auth Service
 *
 * Role:
 * - JWT 토큰 재발급 및 로그아웃 처리를 담당하는 비즈니스 로직 계층
 * - Refresh Token을 이용한 Access Token 갱신
 * - 로그아웃 시 Refresh Token 무효화
 *
 * Complexity/Rationale:
 * 1. Refresh Token 기반 인증:
 *    - Access Token(10분)은 짧게, Refresh Token(7일)은 길게 설정
 *    - Access Token 만료 시 Refresh Token으로 재발급 (재로그인 불필요)
 *    - Refresh Token은 DB에 저장하여 서버에서 검증 가능
 *
 * 2. 보안 고려사항:
 *    - Refresh Token 탈취 방지: DB에 저장된 토큰과 비교 검증
 *    - 로그아웃 시 DB에서 삭제하여 무효화
 *    - 만료된 Refresh Token은 사용 불가
 *
 * 3. 트랜잭션 관리:
 *    - @Transactional: DB 작업(조회, 저장, 삭제) 원자성 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * Refresh Token으로 Access Token 재발급
     *
     * Role:
     * - Access Token 만료 시 Refresh Token을 이용하여 새로운 Access Token 발급
     * - Refresh Token도 함께 갱신 (Refresh Token Rotation)
     *
     * Complexity/Rationale:
     * 1. Refresh Token 검증 흐름:
     *    - DB에 저장된 Refresh Token과 일치하는지 확인
     *    - Refresh Token 만료 여부 확인
     *    - Refresh Token이 유효하면 새로운 토큰 세트 발급
     *
     * 2. Refresh Token Rotation:
     *    - 새로운 Access Token과 Refresh Token을 함께 발급
     *    - 기존 Refresh Token은 새로운 토큰으로 교체
     *    - 보안 강화: Refresh Token 탈취 시에도 사용 횟수 제한
     *
     * 3. 예외 처리:
     *    - Refresh Token이 DB에 없는 경우 → YamoyoException(INVALID_REFRESH_TOKEN)
     *    - Refresh Token이 만료된 경우 → YamoyoException(INVALID_REFRESH_TOKEN)
     *    - 사용자를 찾을 수 없는 경우 → YamoyoException(USER_NOT_FOUND)
     *
     * @param refreshToken Refresh Token 문자열
     * @return JwtTokenDto 새로운 Access Token + Refresh Token
     * @throws YamoyoException Refresh Token 검증 실패 시
     */
    @Transactional
    public JwtTokenDto refresh(String refreshToken) {
        log.info("Refresh Token으로 Access Token 재발급 시도");

        // 1. DB에서 Refresh Token 조회
        RefreshToken storedRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new YamoyoException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 2. Refresh Token 만료 여부 확인
        if (storedRefreshToken.isExpired()) {
            // 만료된 Refresh Token은 DB에서 삭제
            refreshTokenRepository.delete(storedRefreshToken);
            throw new YamoyoException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. Refresh Token에서 사용자 정보 추출
        // Refresh Token도 JWT이므로 JwtTokenProvider로 파싱 가능
        // 만료된 토큰일 수 있으므로 parseClaimsFromExpiredToken 사용
        JwtTokenClaims claims = jwtTokenProvider.parseClaimsFromExpiredToken(refreshToken);

        // 4. DB에서 사용자 조회
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        log.info("Refresh Token 검증 완료 - UserId: {}, Email: {}", user.getId(), user.getEmail());

        // 5. 새로운 Access Token + Refresh Token 생성 (Refresh Token Rotation)
        JwtTokenDto newTokens = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getProvider()
        );

        // 6. DB에 새로운 Refresh Token 저장 (기존 토큰 교체)
        LocalDateTime newExpiryDate = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);
        storedRefreshToken.updateToken(newTokens.refreshToken(), newExpiryDate);

        log.info("새로운 토큰 발급 완료 - UserId: {}", user.getId());

        return newTokens;
    }

    /**
     * 로그아웃 처리
     *
     * Role:
     * - DB에서 Refresh Token을 삭제하여 무효화
     * - Access Token은 stateless이므로 서버에서 무효화 불가 (만료 시까지 유효)
     *
     * Complexity/Rationale:
     * 1. Refresh Token 무효화:
     *    - DB에서 userId에 해당하는 Refresh Token 삭제
     *    - 이후 해당 Refresh Token으로는 Access Token 재발급 불가
     *
     * 2. Access Token 처리:
     *    - Access Token은 stateless JWT이므로 서버에서 무효화 불가
     *    - 만료 시간(10분)까지는 유효하게 사용 가능
     *    - 보안이 중요한 경우 Access Token 만료 시간을 짧게 설정
     *
     * 3. 예외 처리:
     *    - Refresh Token이 없어도 로그아웃 성공 처리 (멱등성)
     *    - 이미 로그아웃한 사용자가 다시 로그아웃 시도해도 정상 처리
     *
     * @param userId 로그아웃할 사용자 ID
     */
    @Transactional
    public void logout(Long userId) {
        log.info("로그아웃 처리 - UserId: {}", userId);

        // DB에서 Refresh Token 삭제
        // deleteByUserId는 JPA가 자동으로 트랜잭션 내에서 처리
        // Refresh Token이 없어도 예외 발생하지 않음 (멱등성)
        refreshTokenRepository.deleteByUserId(userId);

        log.info("로그아웃 완료 - UserId: {}", userId);
    }
}
