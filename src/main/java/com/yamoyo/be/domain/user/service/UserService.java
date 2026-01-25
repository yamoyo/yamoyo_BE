package com.yamoyo.be.domain.user.service;

import com.yamoyo.be.domain.user.dto.ProfileSetupRequest;
import com.yamoyo.be.domain.user.dto.TermsAgreementRequest;
import com.yamoyo.be.domain.user.dto.TermsAgreementRequest.TermAgreement;
import com.yamoyo.be.domain.user.entity.Term;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.entity.UserAgreement;
import com.yamoyo.be.domain.user.repository.TermRepository;
import com.yamoyo.be.domain.user.repository.UserAgreementRepository;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User Service
 *
 * Role:
 * - 사용자 관련 비즈니스 로직 처리
 * - 온보딩 과정의 약관 동의, 프로필 설정 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserAgreementRepository userAgreementRepository;

    @Transactional
    public void agreeToTerms(Long userId, TermsAgreementRequest request) {
        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // 2. 필수 약관 검증
        List<Term> mandatoryTerms = termRepository.findByIsActiveAndIsMandatory(true, true);
        Set<Long> mandatoryTermIds = mandatoryTerms.stream()
                .map(Term::getId)
                .collect(Collectors.toSet());

        // 요청에서 동의한 약관 ID 추출
        Map<Long, Boolean> agreedMap = request.agreements().stream()
                .collect(Collectors.toMap(TermAgreement::termsId, TermAgreement::agreed));

        // 모든 필수 약관에 동의했는지 확인
        boolean allMandatoryAgreed = mandatoryTermIds.stream()
                .allMatch(termId -> Boolean.TRUE.equals(agreedMap.get(termId)));

        if (!allMandatoryAgreed) {
            log.warn("필수 약관 미동의 - UserId: {}, MandatoryTermIds: {}, AgreedMap: {}",
                    userId, mandatoryTermIds, agreedMap);
            throw new YamoyoException(ErrorCode.MANDATORY_TERMS_NOT_AGREED);
        }

        // 3. UserAgreement 저장
        Set<Long> requestedTermIds = request.agreements().stream()
                .map(TermAgreement::termsId)
                .collect(Collectors.toSet());

        Map<Long, Term> foundTerms = termRepository.findAllById(requestedTermIds).stream()
                .collect(Collectors.toMap(Term::getId, term -> term));

        if (foundTerms.size() != requestedTermIds.size()) {
            throw new YamoyoException(ErrorCode.TERMS_NOT_FOUND);
        }

        List<UserAgreement> agreements = request.agreements().stream()
                .map(agreement -> {
                    Term term = foundTerms.get(agreement.termsId());
                    return UserAgreement.create(user, term, agreement.agreed());
                })
                .toList();

        userAgreementRepository.saveAll(agreements);

        log.info("약관 동의 완료 - UserId: {}, AgreementCount: {}", userId, agreements.size());
    }

    @Transactional
    public void setupProfile(Long userId, ProfileSetupRequest request) {
        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // 2. 약관 동의 여부 확인
        boolean hasAgreedToTerms = userAgreementRepository.hasAgreedToAllMandatoryTerms(userId);
        if (!hasAgreedToTerms) {
            log.warn("약관 미동의 상태에서 프로필 설정 시도 - UserId: {}", userId);
            throw new YamoyoException(ErrorCode.TERMS_NOT_AGREED);
        }

        // 3. 프로필 정보 업데이트
        user.updateName(request.name());
        user.updateMajor(request.major());
        user.updateMBTI(request.mbti());
        user.updateProfileImageId(request.profileImageId());

        // 4. UserRole 변경 (GUEST → USER)
        user.completeOnboarding();

        log.info("프로필 설정 완료 - UserId: {}, Name: {}, Major: {}, MBTI: {}, UserRole: {}",
                userId, request.name(), request.major(), request.mbti(), user.getUserRole());
    }
}
