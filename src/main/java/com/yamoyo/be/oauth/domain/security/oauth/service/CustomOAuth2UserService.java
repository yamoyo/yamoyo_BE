package com.yamoyo.be.oauth.domain.security.oauth.service;

import com.yamoyo.be.oauth.domain.security.oauth.CustomOAuth2User;
import com.yamoyo.be.oauth.domain.security.oauth.OAuth2Provider;
import com.yamoyo.be.oauth.domain.security.oauth.OAuth2UserInfo;
import com.yamoyo.be.oauth.domain.user.entity.Role;
import com.yamoyo.be.oauth.domain.user.entity.User;
import com.yamoyo.be.oauth.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom OAuth2UserService
 *
 * Role:
 * - OAuth2 로그인 성공 후 사용자 정보를 가져와서 DB에 저장/업데이트
 * - Spring Security의 DefaultOAuth2UserService를 확장
 *
 * Complexity/Rationale:
 * 1. Factory Pattern & Strategy Pattern 적용:
 *    - OAuth2Provider.getOAuth2UserInfo()를 통해 Provider별 구현체 획득
 *    - Service 계층은 OAuth2UserInfo 인터페이스에만 의존
 *    - 새로운 Provider 추가 시 이 클래스 수정 불필요 (Open-Closed Principle)
 *
 * 2. OAuth2 로그인 플로우:
 *    - Spring Security가 OAuth2 인증 완료 후 이 Service의 loadUser() 호출
 *    - Provider로부터 받은 사용자 정보로 회원가입 또는 정보 업데이트
 *    - CustomOAuth2User 객체를 반환하여 SecurityContext에 저장
 *
 * 3. 트랜잭션 관리:
 *    - @Transactional: DB 작업(회원가입/업데이트) 원자성 보장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * OAuth2 Provider로부터 사용자 정보 로드
     *
     * Role:
     * - Spring Security가 OAuth2 인증 완료 후 호출하는 메서드
     * - Provider로부터 받은 사용자 정보로 회원가입/로그인 처리
     *
     * Complexity/Rationale:
     * 1. 사용자 정보 가져오기:
     *    - super.loadUser()로 Provider의 UserInfo API 호출
     *    - OAuth2User 객체로 사용자 정보 획득
     *
     * 2. Provider 식별 및 파싱:
     *    - registrationId("google", "kakao")로 어떤 Provider인지 판별
     *    - OAuth2Provider.getOAuth2UserInfo()로 적절한 구현체 획득
     *    - 이 과정에서 if-else 분기 없이 Factory Pattern 활용
     *
     * 3. 회원가입/로그인:
     *    - saveOrUpdate()로 기존 사용자면 업데이트, 신규면 저장
     *
     * 4. CustomOAuth2User 반환:
     *    - Spring Security가 SecurityContext에 저장
     *    - Controller에서 @AuthenticationPrincipal로 주입 가능
     *
     * @param userRequest OAuth2 로그인 요청 정보
     * @return OAuth2User Spring Security가 사용할 사용자 정보
     * @throws OAuth2AuthenticationException OAuth2 인증 실패 시
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Provider로부터 사용자 정보 가져오기
        // super.loadUser()는 Provider의 UserInfo Endpoint를 호출하여 사용자 정보 획득
        OAuth2User oauth2User = super.loadUser(userRequest);

        // 2. Provider 식별
        // registrationId: application.yml의 spring.security.oauth2.client.registration.{registrationId}
        // 예: "google", "kakao", "naver"
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 3. Provider에서 사용자를 식별하는 attribute key 추출
        // Google: "sub", Kakao: "id"
        // application.yml의 user-name-attribute 설정값
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // 4. Provider별 사용자 정보 파싱 (Factory Pattern 적용)
        // registrationId를 기반으로 적절한 OAuth2UserInfo 구현체 획득
        // Google → GoogleOAuth2UserInfo, Kakao → KakaoOAuth2UserInfo
        // 이 과정에서 if-else 분기 없이 Enum의 다형성 활용
        Map<String, Object> attributes = oauth2User.getAttributes();
        OAuth2UserInfo oAuth2UserInfo = OAuth2Provider.getOAuth2UserInfo(registrationId, attributes);

        // 로그: 어떤 Provider로 로그인했는지, 사용자 ID는 무엇인지
        log.info("OAuth2 로그인 - Provider: {}, ProviderId: {}, Email: {}",
                oAuth2UserInfo.getProvider(),
                oAuth2UserInfo.getProviderId(),
                oAuth2UserInfo.getEmail());

        // 5. 회원가입 또는 정보 업데이트
        // Provider + ProviderId로 기존 사용자 조회
        // 있으면 정보 업데이트, 없으면 신규 회원가입
        User user = saveOrUpdate(oAuth2UserInfo);

        // 6. CustomOAuth2User 반환
        // Spring Security가 SecurityContext에 저장
        // Controller에서 @AuthenticationPrincipal CustomOAuth2User로 주입 가능

        // 변경: attributes 맵을 수정 가능한 맵으로 복사하여 정규화된 데이터("name", "email", "picture")를 추가
        // 이를 통해 Controller에서 Provider 구분 없이 동일한 키로 접근 가능하게 함
        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put("name", oAuth2UserInfo.getName());
        newAttributes.put("email", oAuth2UserInfo.getEmail());
        newAttributes.put("picture", oAuth2UserInfo.getPicture());

        return new CustomOAuth2User(
                user.getRole(),
                newAttributes,
                userNameAttributeName
        );
    }

    /**
     * 사용자 저장 또는 업데이트
     *
     * Role:
     * - OAuth2 로그인 시 기존 사용자면 정보 업데이트, 신규면 회원가입
     *
     * Complexity/Rationale:
     * - provider + providerId로 사용자 조회
     * - 기존 사용자: 이름/이메일/프로필 이미지만 업데이트 (Provider는 변경 불가)
     * - 신규 사용자: User 엔티티 생성 후 DB 저장, 기본 권한은 USER
     *
     * @param oAuth2UserInfo Provider별 사용자 정보 (인터페이스)
     * @return User 저장된 사용자 엔티티
     */
    private User saveOrUpdate(OAuth2UserInfo oAuth2UserInfo) {
        // provider + providerId로 기존 사용자 조회
        User user = userRepository.findByProviderAndProviderId(
                        oAuth2UserInfo.getProvider(),
                        oAuth2UserInfo.getProviderId())
                // 기존 사용자면 정보 업데이트
                .map(existingUser -> existingUser.update(
                        oAuth2UserInfo.getName(),
                        oAuth2UserInfo.getEmail(),
                        oAuth2UserInfo.getPicture()))
                // 신규 사용자면 회원가입
                .orElse(User.builder()
                        .name(oAuth2UserInfo.getName())
                        .email(oAuth2UserInfo.getEmail())
                        .provider(oAuth2UserInfo.getProvider())
                        .providerId(oAuth2UserInfo.getProviderId())
                        .picture(oAuth2UserInfo.getPicture())
                        .role(Role.USER)  // 기본 권한: USER
                        .build());

        // DB 저장 (기존 사용자면 UPDATE, 신규면 INSERT)
        return userRepository.save(user);
    }
}
