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
 * - email 기준으로 사용자 조회
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

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
}
