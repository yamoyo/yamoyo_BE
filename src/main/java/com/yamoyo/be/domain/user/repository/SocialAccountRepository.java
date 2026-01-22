package com.yamoyo.be.domain.user.repository;

import com.yamoyo.be.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SocialAccount Repository
 *
 * Role:
 * - SocialAccount 엔티티에 대한 데이터베이스 접근 계층
 * - provider + providerId로 소셜 계정 조회
 */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * Provider와 ProviderId로 소셜 계정 조회
     *
     * @param provider OAuth2 Provider 이름 (예: "google", "kakao")
     * @param providerId OAuth2 Provider의 사용자 고유 ID
     * @return Optional<SocialAccount> 소셜 계정 (없으면 empty)
     */
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Provider와 ProviderId로 소셜 계정과 연결된 User 함께 조회
     */
    @Query("SELECT sa FROM SocialAccount sa JOIN FETCH sa.user WHERE sa.provider = :provider AND sa.providerId = :providerId")
    Optional<SocialAccount> findByProviderAndProviderIdWithUser(@Param("provider") String provider, @Param("providerId") String providerId);

    /**
     * 해당 소셜 계정이 존재하는지 확인
     */
    boolean existsByProviderAndProviderId(String provider, String providerId);
}
