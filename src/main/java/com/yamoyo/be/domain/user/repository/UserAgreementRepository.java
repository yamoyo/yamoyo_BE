package com.yamoyo.be.domain.user.repository;

import com.yamoyo.be.domain.user.entity.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * UserAgreement Repository
 *
 * Role:
 * - UserAgreement 엔티티에 대한 데이터베이스 접근 계층
 * - 사용자의 필수 약관 동의 여부 확인
 */
@Repository
public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {

    /**
     * 사용자가 모든 필수 약관에 동의했는지 확인
     *
     * @param userId 사용자 ID
     * @return 모든 필수 약관에 동의했으면 true
     */
    @Query("""
        SELECT CASE
            WHEN COUNT(t) = 0 THEN true
            WHEN COUNT(ua) = COUNT(t) THEN true
            ELSE false
        END
        FROM Term t
        LEFT JOIN UserAgreement ua ON ua.term = t AND ua.user.id = :userId AND ua.isAgreed = true
        WHERE t.isActive = true AND t.isMandatory = true
        """)
    boolean hasAgreedToAllMandatoryTerms(@Param("userId") Long userId);
}
