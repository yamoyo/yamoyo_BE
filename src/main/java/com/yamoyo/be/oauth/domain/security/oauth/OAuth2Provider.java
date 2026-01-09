package com.yamoyo.be.oauth.domain.security.oauth;

import java.util.Arrays;
import java.util.Map;

/**
 * OAuth2 Provider Enum
 *
 * Role:
 * - OAuth2 Provider(Google, Kakao, Naver 등)를 열거형으로 관리
 * - Factory Method Pattern: registrationId를 기반으로 적절한 OAuth2UserInfo 구현체 생성
 *
 * Complexity/Rationale:
 * 1. Factory Method Pattern 적용:
 *    - registrationId("google", "kakao")를 입력받아 해당 Provider의 OAuth2UserInfo 구현체 반환
 *    - if-else 분기 대신 Enum의 다형성을 활용하여 코드 확장성 향상
 *
 * 2. Strategy Pattern 적용:
 *    - 각 Provider마다 다른 파싱 로직(GoogleOAuth2UserInfo, KakaoOAuth2UserInfo)을 전략으로 캡슐화
 *    - Service 계층은 OAuth2UserInfo 인터페이스에만 의존
 *
 * 3. 확장성:
 *    - 새로운 Provider(예: Naver) 추가 시 Enum 상수만 추가하면 됨
 *    - Service 코드 수정 불필요 (Open-Closed Principle)
 *
 * 사용 예시:
 * <pre>
 * String registrationId = "google";
 * Map<String, Object> attributes = oauth2User.getAttributes();
 * OAuth2UserInfo userInfo = OAuth2Provider.getOAuth2UserInfo(registrationId, attributes);
 * </pre>
 */
public enum OAuth2Provider {

    /**
     * Google OAuth2 Provider
     *
     * Role:
     * - Google 로그인 시 GoogleOAuth2UserInfo 구현체 생성
     * - registrationId: "google"
     */
    GOOGLE {
        @Override
        public OAuth2UserInfo getOAuth2UserInfo(Map<String, Object> attributes) {
            return new GoogleOAuth2UserInfo(attributes);
        }
    },

    /**
     * Kakao OAuth2 Provider
     *
     * Role:
     * - Kakao 로그인 시 KakaoOAuth2UserInfo 구현체 생성
     * - registrationId: "kakao"
     */
    KAKAO {
        @Override
        public OAuth2UserInfo getOAuth2UserInfo(Map<String, Object> attributes) {
            return new KakaoOAuth2UserInfo(attributes);
        }
    };

    /**
     * OAuth2UserInfo 구현체 생성 (추상 메서드)
     *
     * Role:
     * - 각 Provider에 맞는 OAuth2UserInfo 구현체를 생성하는 Factory Method
     * - 각 Enum 상수가 이 메서드를 구현하여 다른 구현체 반환
     *
     * @param attributes OAuth2 Provider로부터 받은 사용자 정보
     * @return OAuth2UserInfo 구현체
     */
    public abstract OAuth2UserInfo getOAuth2UserInfo(Map<String, Object> attributes);

    /**
     * registrationId를 기반으로 OAuth2UserInfo 구현체 생성 (Static Factory Method)
     *
     * Role:
     * - Spring Security의 registrationId("google", "kakao")를 받아서
     *   해당 Provider의 OAuth2UserInfo 구현체 반환
     *
     * Complexity/Rationale:
     * - registrationId를 대문자로 변환하여 Enum 상수와 매칭
     * - 지원하지 않는 Provider인 경우 IllegalArgumentException 발생
     *
     * 사용 예시:
     * <pre>
     * // Spring Security의 OAuth2User에서 추출
     * String registrationId = "google";
     * Map<String, Object> attributes = oauth2User.getAttributes();
     *
     * // Factory Method로 적절한 구현체 생성
     * OAuth2UserInfo userInfo = OAuth2Provider.getOAuth2UserInfo(registrationId, attributes);
     * </pre>
     *
     * @param registrationId OAuth2 Provider 식별자 ("google", "kakao" 등)
     * @param attributes OAuth2 Provider로부터 받은 사용자 정보
     * @return OAuth2UserInfo 구현체
     * @throws IllegalArgumentException 지원하지 않는 Provider인 경우
     */
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        // registrationId를 대문자로 변환하여 Enum 상수 찾기
        // 예: "google" → GOOGLE, "kakao" → KAKAO
        String providerName = registrationId.toUpperCase();

        // Enum 상수 배열에서 해당 Provider 찾기
        return Arrays.stream(values())
                .filter(provider -> provider.name().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 OAuth2 Provider입니다: " + registrationId))
                // 찾은 Enum 상수의 getOAuth2UserInfo() 호출하여 구현체 생성
                .getOAuth2UserInfo(attributes);
    }
}
