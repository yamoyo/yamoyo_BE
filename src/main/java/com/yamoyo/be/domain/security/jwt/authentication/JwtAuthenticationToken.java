package com.yamoyo.be.domain.security.jwt.authentication;

import com.yamoyo.be.domain.security.jwt.JwtTokenClaims;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * JWT Authentication Token
 *
 * Role:
 * - JWT 토큰 기반 인증 정보를 담는 Authentication 객체
 * - Spring Security의 SecurityContext에 저장됨
 * - JwtAuthenticationFilter에서 생성하여 SecurityContextHolder에 설정
 *
 * Complexity/Rationale:
 * - AbstractAuthenticationToken을 상속받아 Spring Security와 통합
 * - JwtTokenClaims를 principal로 저장하여 사용자 정보 접근 가능
 * - Controller에서 @AuthenticationPrincipal로 주입받을 수 있음
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * Principal: 인증된 사용자의 정보
     * - JwtTokenClaims 객체 (userId, email, role, provider)
     */
    private final Object principal;

    /**
     * Credentials: 자격증명 (JWT는 토큰 자체가 자격증명이므로 null)
     */
    private final Object credentials;

    /**
     * 인증 전 생성자 (사용하지 않음, JWT는 인증 후 바로 생성)
     *
     * @param principal JWT Token Claims
     * @param credentials null (JWT는 토큰 자체가 자격증명)
     */
    private JwtAuthenticationToken(Object principal, Object credentials) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    /**
     * 인증 완료 후 생성자 (주로 사용)
     *
     * Role:
     * - JwtAuthenticationFilter에서 토큰 검증 후 호출
     * - 이미 인증된 상태로 생성 (setAuthenticated(true))
     *
     * @param principal JWT Token Claims
     * @param credentials null
     * @param authorities 사용자 권한 (ROLE_USER, ROLE_ADMIN 등)
     */
    private JwtAuthenticationToken(Object principal, Object credentials,
                                   Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);  // 반드시 인증 완료 상태로 설정
    }

    /**
     * 인증 완료된 JwtAuthenticationToken 생성 (Static Factory Method)
     *
     * Role:
     * - JwtAuthenticationFilter에서 호출
     * - JwtTokenClaims에서 role을 추출하여 GrantedAuthority 생성
     *
     * @param claims JWT 토큰에서 추출한 사용자 정보
     * @return 인증 완료된 JwtAuthenticationToken
     */
    public static JwtAuthenticationToken authenticated(JwtTokenClaims claims) {
        // 팀룸별로 권한이 다르므로, 글로벌 인증 객체에는 권한 목록을 비워둡니다.
        // "인증(Authentication)"은 되었으나, 특정 리소스에 대한 "인가(Authorization)"는 서비스 계층에서 수행합니다.
        return new JwtAuthenticationToken(claims, null, Collections.emptyList());
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    /**
     * JwtTokenClaims를 반환하는 편의 메서드
     *
     * @return JwtTokenClaims
     */
    public JwtTokenClaims getJwtClaims() {
        return (JwtTokenClaims) this.principal;
    }

    /**
     * convertAndSendToUser에서 사용하는 사용자 식별자 반환
     * - Spring WebSocket의 User Destination 기능에서 Principal.getName()을 사용
     *
     * @return userId를 문자열로 반환
     */
    @Override
    public String getName() {
        if (principal instanceof JwtTokenClaims claims) {
            return claims.userId().toString();
        }
        return super.getName();
    }
}
