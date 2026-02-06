package com.yamoyo.be.domain.security.oauth;

import java.util.Map;

/**
 * Google OAuth2 사용자 정보 구현체
 *
 * Role:
 * - Google OAuth2 Provider의 UserInfo 응답 데이터를 파싱
 * - OAuth2UserInfo 인터페이스를 구현하여 통일된 방식으로 데이터 제공
 *
 * Complexity/Rationale:
 * - Google의 UserInfo 응답 구조는 평탄한(flat) 형태
 * - 예시 JSON:
 *   {
 *     "sub": "1234567890",              // 사용자 고유 ID
 *     "name": "홍길동",                  // 사용자 이름
 *     "email": "hong@example.com",      // 이메일
 *     "picture": "https://...",         // 프로필 이미지
 *     "email_verified": true
 *   }
 *
 * 참고:
 * - Google OAuth2 UserInfo API 문서
 *   https://developers.google.com/identity/protocols/oauth2/openid-connect#obtainuserinfo
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    /**
     * Google OAuth2 Provider로부터 받은 원본 attributes
     */
    private final Map<String, Object> attributes;

    /**
     * GoogleOAuth2UserInfo 생성자
     *
     * @param attributes Google OAuth2 UserInfo 응답 데이터
     */
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * Google 사용자 고유 ID 반환
     *
     * Role:
     * - Google에서 제공하는 "sub" (Subject) 필드 값 반환
     * - 이 값은 Google 계정 내에서 영구적이고 고유한 식별자
     *
     * @return Google 사용자 고유 ID
     */
    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    /**
     * Provider 이름 반환
     *
     * @return "google"
     */
    @Override
    public String getProvider() {
        return "google";
    }

    /**
     * 사용자 이메일 주소 반환
     *
     * Role:
     * - Google에서 제공하는 "email" 필드 값 반환
     * - Google은 기본적으로 이메일을 제공 (scope에 email 포함 시)
     *
     * @return 이메일 주소
     */
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    /**
     * 사용자 이름 반환
     *
     * Role:
     * - Google에서 제공하는 "name" 필드 값 반환
     * - 사용자의 전체 이름 (예: "홍길동")
     *
     * @return 사용자 이름
     */
    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    /**
     * 프로필 이미지 URL 반환
     *
     * Role:
     * - Google에서 제공하는 "picture" 필드 값 반환
     * - 사용자의 프로필 이미지 URL
     *
     * @return 프로필 이미지 URL
     */
    @Override
    public String getPicture() {
        return (String) attributes.get("picture");
    }

    /**
     * 원본 attributes 반환
     *
     * @return Google OAuth2 UserInfo 원본 데이터
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
