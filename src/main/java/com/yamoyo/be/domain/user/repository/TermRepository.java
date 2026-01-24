package com.yamoyo.be.domain.user.repository;

import com.yamoyo.be.domain.user.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Term Repository
 *
 * Role:
 * - Term 엔티티에 대한 데이터베이스 접근 계층
 * - 활성화된 필수 약관 조회
 */
@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    /**
     * 활성화된 필수 약관 목록 조회
     *
     * @return 활성화된 필수 약관 목록
     */
    List<Term> findByIsActiveAndIsMandatory(Boolean isActive, Boolean isMandatory);
}
