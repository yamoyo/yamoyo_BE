package com.yamoyo.be.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SocialAccount Entity
 *
 * Role:
 * - 소셜 로그인 계정 정보를 저장하는 엔티티
 * - 같은 이메일의 소셜 계정은 같은 User에 연결됨
 * - Google, Kakao 등 여러 Provider의 계정을 하나의 User로 통합 관리
 */
@Table(name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OAuth2 Provider 식별자
     * 예: "google", "kakao", "naver"
     */
    @Column(nullable = false)
    private String provider;

    /**
     * OAuth2 Provider에서 제공하는 사용자 고유 ID
     * 예: Google의 "sub", Kakao의 "id"
     */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    /**
     * 소셜 계정 이메일
     */
    @Column(name = "email")
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 정적 팩토리 메서드 - 소셜 계정 생성
     */
    public static SocialAccount create(String provider, String providerId, String email) {
        SocialAccount socialAccount = new SocialAccount();
        socialAccount.provider = provider;
        socialAccount.providerId = providerId;
        socialAccount.email = email;
        return socialAccount;
    }

    /**
     * User 연결 설정 (User.addSocialAccount에서 호출)
     */
    void setUser(User user) {
        this.user = user;
    }
}
