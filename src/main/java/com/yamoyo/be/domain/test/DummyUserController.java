package com.yamoyo.be.domain.test;

import com.yamoyo.be.common.dto.ApiResponse;
import com.yamoyo.be.domain.security.jwt.JwtProperties;
import com.yamoyo.be.domain.security.jwt.JwtTokenDto;
import com.yamoyo.be.domain.security.jwt.JwtTokenProvider;
import com.yamoyo.be.domain.security.refreshtoken.RefreshToken;
import com.yamoyo.be.domain.security.refreshtoken.RefreshTokenRepository;
import com.yamoyo.be.domain.user.entity.SocialAccount;
import com.yamoyo.be.domain.user.entity.Term;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.entity.UserAgreement;
import com.yamoyo.be.domain.user.repository.TermRepository;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class DummyUserController {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    private static final String[] NAMES = {
            "테스트유저1", "테스트유저2", "테스트유저3", "테스트유저4", "테스트유저5",
            "테스트유저6", "테스트유저7", "테스트유저8", "테스트유저9", "테스트유저10"
    };

    private static final String[] MAJORS = {
            "컴퓨터공학", "경영학", "전자공학", "심리학", "디자인학", "경제학", "수학", "물리학"
    };

    private static final String[] MBTIS = {
            "INTJ", "INTP", "ENTJ", "ENTP", "INFJ", "INFP", "ENFJ", "ENFP",
            "ISTJ", "ISFJ", "ESTJ", "ESFJ", "ISTP", "ISFP", "ESTP", "ESFP"
    };

    @PostMapping("/dummy-user")
    @Transactional
    public ApiResponse<DummyUserResponse> createDummyUser() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        String email = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@yamoyo.test";
        String name = NAMES[random.nextInt(NAMES.length)];
        String major = MAJORS[random.nextInt(MAJORS.length)];
        String mbti = MBTIS[random.nextInt(MBTIS.length)];
        long profileImageId = random.nextLong(1, 6);

        // User 생성 및 프로필 설정
        User user = User.create(email, name);
        user.updateMajor(major);
        user.updateMBTI(mbti);
        user.updateProfileImageId(profileImageId);
        user.completeOnboarding();

        // SocialAccount 생성
        SocialAccount socialAccount = SocialAccount.create("test", UUID.randomUUID().toString(), email);
        user.addSocialAccount(socialAccount);

        // User 저장 (cascade로 SocialAccount도 저장)
        userRepository.save(user);

        // 필수 약관 동의 처리
        List<Term> mandatoryTerms = termRepository.findByIsActiveAndIsMandatory(true, true);
        for (Term term : mandatoryTerms) {
            UserAgreement agreement = UserAgreement.create(user, term, true);
            userAgreementRepository.save(agreement);
        }

        // JWT 토큰 생성
        JwtTokenDto tokenDto = jwtTokenProvider.generateToken(user.getId(), email, "test");

        // RefreshToken DB 저장
        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtProperties.refreshTokenExpiration() / 1000);
        RefreshToken refreshToken = RefreshToken.create(user.getId(), tokenDto.refreshToken(), expiryDate);
        refreshTokenRepository.save(refreshToken);

        DummyUserResponse response = new DummyUserResponse(
                user.getId(),
                email,
                name,
                major,
                mbti,
                tokenDto.accessToken(),
                tokenDto.refreshToken()
        );

        return ApiResponse.success(response);
    }
}
