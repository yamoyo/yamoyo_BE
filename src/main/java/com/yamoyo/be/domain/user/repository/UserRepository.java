package com.yamoyo.be.domain.user.repository;

import com.yamoyo.be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository
 *
 * Role:
 * - User 엔티티에 대한 데이터베이스 접근 계층
 * - JpaRepository를 상속받아 기본 CRUD 기능 제공
 *
 * Complexity/Rationale:
 * - provider + providerId 조합으로 사용자를 찾는 커스텀 쿼리 메서드 제공
 * - OAuth2 로그인 시 기존 사용자인지 신규 사용자인지 판별하는데 사용
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Provider와 ProviderId로 사용자 조회
     *
     * Role:
     * - OAuth2 로그인 시 해당 Provider로 이미 가입한 사용자인지 확인
     *
     * Complexity/Rationale:
     * - provider와 providerId의 조합은 사용자를 고유하게 식별
     * - 예: provider="google", providerId="1234567890"
     * - 같은 providerId라도 다른 provider면 다른 사용자
     *
     * 사용 예시:
     * <pre>
     * Optional<User> existingUser = userRepository.findByProviderAndProviderId("google", "1234567890");
     * if (existingUser.isPresent()) {
     *     // 기존 사용자 → 정보 업데이트
     * } else {
     *     // 신규 사용자 → 회원가입
     * }
     * </pre>
     *
     * @param provider OAuth2 Provider 이름 (예: "google", "kakao")
     * @param providerId OAuth2 Provider의 사용자 고유 ID
     * @return Optional<User> 사용자 (없으면 empty)
     */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
