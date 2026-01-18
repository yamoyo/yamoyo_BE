package com.yamoyo.be.domain.security.oauth;

import java.util.Map;

/**
 * Kakao OAuth2 사용자 정보 구현체
 *
 * Role:
 * - Kakao OAuth2 Provider의 UserInfo 응답 데이터를 파싱
 * - OAuth2UserInfo 인터페이스를 구현하여 통일된 방식으로 데이터 제공
 *
 * Complexity/Rationale:
 * - Kakao의 UserInfo 응답 구조는 중첩(nested) 형태
 * - 예시 JSON:
 *   {
 *     "id": 123456789,                          // 사용자 고유 ID (Long 타입)
 *     "kakao_account": {                        // 카카오 계정 정보
 *       "profile": {                            // 프로필 정보
 *         "nickname": "홍길동",                  // 닉네임
 *         "profile_image_url": "https://..."   // 프로필 이미지
 *       },
 *       "email": "hong@example.com"             // 이메일 (선택 동의)
 *     }
 *   }
 *
 * 주의:
 * - email은 사용자가 동의하지 않으면 제공되지 않을 수 있음 (nullable)
 * - id는 Long 타입이므로 String으로 변환 필요
 * - kakao_account와 profile이 null일 수 있으므로 안전하게 파싱
 *
 * 참고:
 * - Kakao REST API 문서
 *   https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    /**
     * Kakao OAuth2 Provider로부터 받은 원본 attributes
     */
    private final Map<String, Object> attributes;

    /**
     * kakao_account 객체 (중첩된 데이터)
     */
    private final Map<String, Object> kakaoAccount;

    /**
     * profile 객체 (kakao_account 내부의 중첩된 데이터)
     */
    private final Map<String, Object> profile;

    /**
     * KakaoOAuth2UserInfo 생성자
     *
     * Role:
     * - Kakao OAuth2 UserInfo 응답 데이터를 파싱
     * - 중첩된 구조(kakao_account, profile)를 미리 추출하여 캐싱
     *
     * Complexity/Rationale:
     * - kakao_account와 profile이 null일 수 있으므로 안전하게 추출
     * - SuppressWarnings("unchecked"): Map 타입 캐스팅 경고 억제
     *
     * @param attributes Kakao OAuth2 UserInfo 응답 데이터
     */
    @SuppressWarnings("unchecked")
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;

        // kakao_account 추출 (없으면 빈 Map)
        this.kakaoAccount = attributes.get("kakao_account") != null
                ? (Map<String, Object>) attributes.get("kakao_account")
                : Map.of();

        // profile 추출 (kakao_account 내부, 없으면 빈 Map)
        this.profile = kakaoAccount.get("profile") != null
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : Map.of();
    }

    /**
     * Kakao 사용자 고유 ID 반환
     *
     * Role:
     * - Kakao에서 제공하는 "id" 필드 값 반환
     * - Long 타입을 String으로 변환
     *
     * Complexity/Rationale:
     * - Kakao의 id는 Long 타입이므로 String.valueOf()로 변환
     * - DB에 저장할 때 String 타입으로 통일
     *
     * @return Kakao 사용자 고유 ID
     */
    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    /**
     * Provider 이름 반환
     *
     * @return "kakao"
     */
    @Override
    public String getProvider() {
        return "kakao";
    }

    /**
     * 사용자 이메일 주소 반환
     *
     * Role:
     * - Kakao에서 제공하는 "kakao_account.email" 필드 값 반환
     *
     * 주의:
     * - 이메일은 사용자가 동의하지 않으면 null일 수 있음
     * - 이메일 제공 동의는 Kakao에서 선택사항
     *
     * @return 이메일 주소 (nullable)
     */
    @Override
    public String getEmail() {
        return (String) kakaoAccount.get("email");
    }

    /**
     * 사용자 이름(닉네임) 반환
     *
     * Role:
     * - Kakao에서 제공하는 "kakao_account.profile.nickname" 필드 값 반환
     * - 사용자의 카카오톡 닉네임
     *
     * @return 사용자 닉네임
     */
    @Override
    public String getName() {
        return (String) profile.get("nickname");
    }

    /**
     * 프로필 이미지 URL 반환
     *
     * Role:
     * - Kakao에서 제공하는 "kakao_account.profile.profile_image_url" 필드 값 반환
     * - 사용자의 카카오톡 프로필 이미지
     *
     * @return 프로필 이미지 URL
     */
    @Override
    public String getPicture() {
        return (String) profile.get("profile_image_url");
    }

    /**
     * 원본 attributes 반환
     *
     * @return Kakao OAuth2 UserInfo 원본 데이터
     */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
