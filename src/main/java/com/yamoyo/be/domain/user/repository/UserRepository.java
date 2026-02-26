package com.yamoyo.be.domain.user.repository;

import com.yamoyo.be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository
 *
 * Role:
 * - User 엔티티에 대한 데이터베이스 접근 계층
 * - email 기준으로 사용자 조회
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    interface OnboardingCheckProjection {
        Boolean getHasAgreedToTerms();
        Boolean getProfileCompleted();
    }

    /**
     * 이메일로 사용자 조회
     *
     * @param email 이메일 주소
     * @return Optional<User> 사용자 (없으면 empty)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 이메일 주소
     * @return 존재하면 true
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 온보딩 상태를 단일 쿼리로 조회
     *
     * @param userId 사용자 ID
     * @return 필수 약관 동의 여부 + 프로필 완료 여부
     */
    @Query("""
        SELECT
            CASE
                WHEN (SELECT COUNT(t) FROM Term t WHERE t.isActive = true AND t.isMandatory = true) = 0 THEN true
                WHEN (
                    SELECT COUNT(ua)
                    FROM UserAgreement ua
                    JOIN ua.term t
                    WHERE ua.user.id = :userId
                    AND ua.isAgreed = true
                    AND t.isActive = true
                    AND t.isMandatory = true
                ) = (
                    SELECT COUNT(t2) FROM Term t2 WHERE t2.isActive = true AND t2.isMandatory = true
                ) THEN true
                ELSE false
            END AS hasAgreedToTerms,
            CASE
                WHEN u.major IS NOT NULL THEN true
                ELSE false
            END AS profileCompleted
        FROM User u
        WHERE u.id = :userId
        """)
    Optional<OnboardingCheckProjection> findOnboardingStatusByUserId(@Param("userId") Long userId);
}
