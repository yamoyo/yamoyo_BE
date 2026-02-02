package com.yamoyo.be.domain.fcm.repository;

import com.yamoyo.be.domain.fcm.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserDevice Repository
 *
 * Role:
 * - UserDevice 엔티티에 대한 데이터베이스 접근 계층
 */
@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    /**
     * 사용자의 모든 기기 조회
     */
    List<UserDevice> findAllByUserId(Long userId);

    /**
     * FCM 토큰으로 기기 조회
     */
    Optional<UserDevice> findByFcmToken(String fcmToken);

    /**
     * 특정 사용자의 특정 FCM 토큰을 가진 기기 조회
     */
    Optional<UserDevice> findByUserIdAndFcmToken(Long userId, String fcmToken);

    /**
     * 특정 사용자의 특정 FCM 토큰을 가진 기기 삭제
     */
    void deleteByUserIdAndFcmToken(Long userId, String fcmToken);

    List<UserDevice> findByUserId(Long userId);

    @Query("SELECT ud FROM UserDevice ud JOIN FETCH ud.user WHERE ud.user.id IN :userIds")
    List<UserDevice> findAllByUserIdIn(@Param("userIds") List<Long> userIds);
}
