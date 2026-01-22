package com.yamoyo.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserDevice Entity
 *
 * Role:
 * - 사용자 기기 정보를 저장하는 엔티티
 * - 푸시 알림, 다중 기기 로그인 관리 등에 활용
 */
@Table(name = "user_devices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_id"}))
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 기기 고유 식별자
     * 예: FCM 토큰, 디바이스 UUID
     */
    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    /**
     * 기기 유형
     * 예: "ANDROID", "IOS", "WEB"
     */
    @Column(name = "device_type")
    private String deviceType;

    /**
     * 기기 이름
     * 예: "iPhone 15 Pro", "Galaxy S24"
     */
    @Column(name = "device_name")
    private String deviceName;

    /**
     * 마지막 로그인 시간
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 정적 팩토리 메서드 - 기기 등록
     */
    public static UserDevice create(User user, String fcmToken, String deviceType, String deviceName) {
        UserDevice userDevice = new UserDevice();
        userDevice.user = user;
        userDevice.fcmToken = fcmToken;
        userDevice.deviceType = deviceType;
        userDevice.deviceName = deviceName;
        return userDevice;
    }

    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
