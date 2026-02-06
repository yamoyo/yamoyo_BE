package com.yamoyo.be.domain.security.oauth.service;

import com.yamoyo.be.domain.security.oauth.CustomOAuth2User;
import com.yamoyo.be.domain.security.oauth.OAuth2Provider;
import com.yamoyo.be.domain.security.oauth.OAuth2UserInfo;
import com.yamoyo.be.domain.user.entity.OnboardingStatus;
import com.yamoyo.be.domain.user.entity.SocialAccount;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.SocialAccountRepository;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.repository.UserRepository;
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
 * - 같은 이메일의 소셜 계정은 같은 User로 통합
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserAgreementRepository userAgreementRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        Map<String, Object> attributes = oauth2User.getAttributes();
        OAuth2UserInfo oAuth2UserInfo = OAuth2Provider.getOAuth2UserInfo(registrationId, attributes);

        log.info("OAuth2 로그인 - Provider: {}, ProviderId: {}, Email: {}",
                oAuth2UserInfo.getProvider(),
                oAuth2UserInfo.getProviderId(),
                oAuth2UserInfo.getEmail());

        User user = saveOrUpdate(oAuth2UserInfo);
        OnboardingStatus onboardingStatus = determineOnboardingStatus(user);

        log.info("온보딩 상태 확인 - UserId: {}, Status: {}", user.getId(), onboardingStatus);

        Map<String, Object> newAttributes = new HashMap<>(attributes);
        newAttributes.put("name", oAuth2UserInfo.getName());
        newAttributes.put("email", oAuth2UserInfo.getEmail());
        newAttributes.put("picture", oAuth2UserInfo.getPicture());
        newAttributes.put("userId", user.getId());
        newAttributes.put("userRole", user.getUserRole());
        newAttributes.put("onboardingStatus", onboardingStatus);

        return new CustomOAuth2User(
                newAttributes,
                userNameAttributeName
        );
    }

    /**
     * 온보딩 상태 결정
     *
     * 로직:
     * 1. 필수 약관 동의 여부 확인
     * 2. 프로필 완료 여부 확인 (major 필드 기준)
     *
     * @param user 사용자 엔티티
     * @return OnboardingStatus
     */
    private OnboardingStatus determineOnboardingStatus(User user) {
        // 1. 필수 약관 동의 여부 확인
        boolean hasAgreedToTerms = userAgreementRepository.hasAgreedToAllMandatoryTerms(user.getId());
        if (!hasAgreedToTerms) {
            return OnboardingStatus.TERMS_PENDING;
        }

        // 2. 프로필 완료 여부 확인 (major 필드가 null이 아니면 완료)
        boolean isProfileCompleted = user.getMajor() != null;
        if (!isProfileCompleted) {
            return OnboardingStatus.PROFILE_PENDING;
        }

        return OnboardingStatus.COMPLETED;
    }

    /**
     * 사용자 저장 또는 업데이트
     *
     * 로직:
     * 1. provider + providerId로 기존 소셜 계정 조회
     * 2. 소셜 계정이 있으면 → 연결된 User 정보 업데이트
     * 3. 소셜 계정이 없으면 → email로 User 조회
     *    - User가 있으면 → 새 소셜 계정을 기존 User에 연결
     *    - User가 없으면 → 새 User + 소셜 계정 생성
     */
    private User saveOrUpdate(OAuth2UserInfo oAuth2UserInfo) {
        String provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();

        // 1. 기존 소셜 계정 조회
        return socialAccountRepository.findByProviderAndProviderIdWithUser(provider, providerId)
                .map(SocialAccount::getUser) // 정보 업데이트 없이 User 반환
                .orElseGet(() -> {
                    // 2. 소셜 계정 없음 -> 이메일로 기존 User 확인
                    User user = userRepository.findByEmail(email)
                            .orElseGet(() -> {
                                // 3. 기존 User도 없음 -> 진짜 신규 생성
                                log.info("신규 사용자 생성 - Email: {}, Provider: {}", email, provider);
                                return User.create(email, name);
                            });

                    // 4. 소셜 계정 연결 (기존 유저든 새 유저든 계정 추가)
                    SocialAccount socialAccount = SocialAccount.create(provider, providerId, email);
                    user.addSocialAccount(socialAccount);

                    return userRepository.save(user);
                });
    }
}
