package com.yamoyo.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User Entity
 *
 * Role:
 * - 사용자 기본 정보를 저장하는 엔티티
 * - email을 기준으로 사용자를 고유하게 식별
 * - 여러 소셜 계정(SocialAccount)이 하나의 User에 연결될 수 있음
 */
@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_image_id")
    private Long profileImageId;

    @Column(name = "major")
    private String major;

    @Column(name = "mbti", columnDefinition = "CHAR(4)")
    private String mbti;

    @Column(name = "is_alarm_on")
    private boolean isAlarmOn;

    @Enumerated(EnumType.STRING)
    private UserRole userRole = UserRole.GUEST;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialAccount> socialAccounts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 정적 팩토리 메서드 - 신규 사용자 생성
     */
    public static User create(String email, String name) {
        User user = new User();
        user.email = email;
        user.name = name;
        return user;
    }

    public void updateProfileImageId(Long profileImageId) {
        if (!Objects.equals(this.profileImageId, profileImageId)) {
            this.profileImageId = profileImageId;
        }
    }

    public void updateName(String name) {
        if (name != null && !name.equals(this.name)) {
            this.name = name;
        }
    }

    public void updateMajor(String major) {
        if (!Objects.equals(this.major, major)) {
            this.major = major;
        }
    }

    public void updateMBTI(String mbti) {
        if (!Objects.equals(this.mbti, mbti)) {
            this.mbti = mbti;
        }
    }

    /**
     * 온보딩 완료 처리
     * - UserRole을 GUEST에서 USER로 변경
     */
    public void completeOnboarding() {
        this.userRole = UserRole.USER;
    }

    /**
     * 소셜 계정 추가
     */
    public void addSocialAccount(SocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
        socialAccount.setUser(this);
    }
}
