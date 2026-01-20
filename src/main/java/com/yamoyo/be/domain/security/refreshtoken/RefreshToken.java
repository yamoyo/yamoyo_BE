package com.yamoyo.be.domain.security.refreshtoken;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh Token Entity
 *
 * Role:
 * - Refresh Token을 DB에 저장하여 관리
 * - Access Token 재발급 시 Refresh Token 유효성 검증용
 *
 * Complexity/Rationale:
 * - userId를 기준으로 Refresh Token 저장 (1:1 관계)
 * - 로그아웃 시 해당 userId의 Refresh Token 삭제
 * - Refresh Token 탈취 방지를 위해 DB에 저장하여 관리
 * - expiryDate로 만료된 토큰 자동 검증 가능
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor // JPA 스펙을 위한 기본 생성자, 접근 레벨은 PROTECTED로 제한
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자 ID (User 엔티티의 ID)
     * - userId를 기준으로 Refresh Token을 조회/삭제
     * - 한 사용자당 하나의 Refresh Token만 유지 (중복 로그인 방지 가능)
     */
    @Column(nullable = false, unique = true)
    private Long userId;

    /**
     * Refresh Token 문자열
     * - JWT 형식의 Refresh Token
     */
    @Column(nullable = false, length = 500)
    private String token;

    /**
     * 만료 일시
     * - Refresh Token의 만료 시간
     * - 현재 시간과 비교하여 만료 여부 확인
     */
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    /**
     * 생성 일시
     * - Refresh Token이 생성된 시간 (로그인 시간)
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 저장 전 자동으로 createdAt 설정
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Refresh Token 만료 여부 확인
     *
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    /**
     * Refresh Token 업데이트
     *
     * Role:
     * - 기존 Refresh Token을 새로운 토큰으로 갱신
     * - Access Token 재발급 시 Refresh Token도 함께 갱신하는 경우 사용
     *
     * @param token 새로운 Refresh Token
     * @param expiryDate 새로운 만료 일시
     */
    public void updateToken(String token, LocalDateTime expiryDate) {
        this.token = token;
        this.expiryDate = expiryDate;
    }
}
