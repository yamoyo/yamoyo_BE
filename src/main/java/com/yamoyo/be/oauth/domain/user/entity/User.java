package com.yamoyo.be.oauth.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User Entity
 *
 * Role:
 * - OAuth2 소셜 로그인 사용자 정보를 저장하는 엔티티
 * - Google, Kakao 등 여러 Provider의 사용자를 통합 관리
 *
 * Complexity/Rationale:
 * - provider + providerId 조합으로 사용자를 고유하게 식별
 * - email은 Provider마다 제공 여부가 다를 수 있으므로 nullable
 * - 같은 이메일이라도 다른 Provider면 다른 사용자로 처리
 */
@Table(name = "users")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    /**
     * OAuth2 Provider에서 제공하는 사용자 이름
     * 예: Google의 경우 "홍길동", Kakao의 경우 닉네임
     */
    @Column(nullable = false)
    private String name;

    /**
     * OAuth2 Provider에서 제공하는 이메일 주소
     * 주의: Kakao는 이메일 제공 동의가 선택사항이므로 nullable
     */
    @Column
    private String email;

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
    @Column(nullable = false)
    private String providerId;

    /**
     * 사용자 권한
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * 프로필 이미지 URL
     */
    @Column
    private String picture;

    /**
     * 사용자 정보 업데이트
     *
     * Role:
     * - OAuth2 로그인 시 Provider에서 받은 최신 정보로 업데이트
     *
     * @param name 사용자 이름
     * @param email 이메일 주소
     * @param picture 프로필 이미지 URL
     * @return 업데이트된 User 객체
     */
    public User update(String name, String email, String picture) {
        this.name = name;
        this.email = email;
        this.picture = picture;
        return this;
    }
}
