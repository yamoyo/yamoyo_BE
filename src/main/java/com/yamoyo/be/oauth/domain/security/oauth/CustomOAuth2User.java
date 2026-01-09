package com.yamoyo.be.oauth.domain.security.oauth;

import com.example.oauth.domain.user.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Custom OAuth2User 구현체
 *
 * Role:
 * - Spring Security의 OAuth2User 인터페이스 구현
 * - OAuth2 로그인 성공 후 SecurityContext에 저장되는 인증 정보
 * - Controller에서 @AuthenticationPrincipal로 주입받을 수 있음
 *
 * Complexity/Rationale:
 * - OAuth2User의 attributes와 우리 도메인의 User 정보를 함께 보관
 * - SecurityContext에 저장되어 세션 동안 유지됨
 * - Controller에서 현재 로그인한 사용자 정보에 접근할 때 사용
 */
@Getter
public class CustomOAuth2User implements OAuth2User {

    /**
     * 사용자 권한 (예: ROLE_USER, ROLE_ADMIN)
     */
    private final Role role;

    /**
     * OAuth2 Provider로부터 받은 원본 attributes
     * 예: Google의 경우 { "sub": "...", "email": "...", "name": "..." }
     */
    private final Map<String, Object> attributes;

    /**
     * OAuth2 Provider에서 사용자를 식별하는 attribute key
     * 예: Google → "sub", Kakao → "id"
     */
    private final String nameAttributeKey;

    /**
     * CustomOAuth2User 생성자
     *
     * @param role 사용자 권한
     * @param attributes OAuth2 Provider 응답 데이터
     * @param nameAttributeKey Provider의 사용자 식별 키
     */
    public CustomOAuth2User(Role role, Map<String, Object> attributes, String nameAttributeKey) {
        this.role = role;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    /**
     * 사용자 권한 목록 반환
     *
     * Role:
     * - Spring Security가 권한 검사 시 사용
     * - @PreAuthorize, hasRole() 등에서 활용
     *
     * @return GrantedAuthority 컬렉션
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Role enum을 "ROLE_" prefix를 붙여 GrantedAuthority로 변환
        // 예: Role.USER → "ROLE_USER"
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    /**
     * OAuth2 Provider의 원본 attributes 반환
     *
     * Role:
     * - Provider가 제공한 모든 사용자 정보에 접근
     * - Controller에서 추가 정보가 필요할 때 사용
     *
     * @return OAuth2 Provider 응답 데이터
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 사용자 식별 이름 반환
     *
     * Role:
     * - Spring Security가 사용자를 식별하는 기본 키
     * - Provider마다 다른 키 사용 (Google: "sub", Kakao: "id")
     *
     * @return 사용자 식별 값 (예: Google의 sub 값, Kakao의 id 값)
     */
    @Override
    public String getName() {
        return String.valueOf(attributes.get(nameAttributeKey));
    }
}
