package com.yamoyo.be.oauth.domain.security.oauth;

import java.util.Map;

/**
 * OAuth2 Provider의 사용자 정보 추상화 인터페이스
 *
 * Role:
 * - Google, Kakao, Naver 등 각 OAuth2 Provider의 응답 데이터 구조 차이를 추상화
 * - 통일된 인터페이스를 제공하여 Service 계층이 Provider에 의존하지 않도록 함
 *
 * Complexity/Rationale:
 * - Strategy Pattern 적용: 각 Provider별로 다른 응답 구조를 동일한 인터페이스로 처리
 * - Google: 평탄한(flat) 구조 → { "sub": "...", "email": "...", "name": "..." }
 * - Kakao: 중첩(nested) 구조 → { "id": 123, "kakao_account": { "profile": { "nickname": "..." } } }
 * - 각 Provider의 구현체가 복잡한 파싱 로직을 캡슐화
 */
public interface OAuth2UserInfo {

    /**
     * OAuth2 Provider에서 제공하는 사용자 고유 ID 반환
     *
     * Role:
     * - Provider 내에서 사용자를 고유하게 식별하는 ID
     *
     * 예시:
     * - Google: "sub" 필드 값 (예: "1234567890")
     * - Kakao: "id" 필드 값 (예: "987654321")
     *
     * @return Provider의 사용자 고유 ID
     */
    String getProviderId();

    /**
     * OAuth2 Provider 이름 반환
     *
     * Role:
     * - 어떤 Provider를 통해 로그인했는지 식별
     *
     * 예시:
     * - "google"
     * - "kakao"
     * - "naver"
     *
     * @return Provider 이름
     */
    String getProvider();

    /**
     * 사용자 이메일 주소 반환
     *
     * Role:
     * - 사용자의 이메일 주소
     *
     * 주의:
     * - Kakao는 이메일 제공 동의가 선택사항이므로 null일 수 있음
     *
     * @return 이메일 주소 (nullable)
     */
    String getEmail();

    /**
     * 사용자 이름 반환
     *
     * Role:
     * - 사용자의 표시 이름
     *
     * 예시:
     * - Google: "name" 필드 (예: "홍길동")
     * - Kakao: "kakao_account.profile.nickname" (예: "길동이")
     *
     * @return 사용자 이름
     */
    String getName();

    /**
     * 프로필 이미지 URL 반환
     *
     * Role:
     * - 사용자의 프로필 이미지 URL
     *
     * 예시:
     * - Google: "picture" 필드
     * - Kakao: "kakao_account.profile.profile_image_url"
     *
     * @return 프로필 이미지 URL (nullable)
     */
    String getPicture();

    /**
     * Provider로부터 받은 원본 attributes 반환
     *
     * Role:
     * - 디버깅이나 추가 정보 추출 시 사용
     *
     * @return OAuth2 Provider의 원본 응답 데이터
     */
    Map<String, Object> getAttributes();
}
