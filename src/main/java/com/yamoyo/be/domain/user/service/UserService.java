package com.yamoyo.be.domain.user.service;

import com.yamoyo.be.domain.user.dto.response.UserResponse;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service
 *
 * Role:
 * - 사용자 프로필 조회 및 수정을 담당하는 비즈니스 로직 계층
 *
 * Complexity/Rationale:
 * 1. 프로필 조회:
 *    - 인증된 사용자 본인의 정보만 조회 가능
 *
 * 2. 프로필 수정:
 *    - 전달된 필드만 업데이트 (null이 아닌 필드만)
 *    - 각 필드별 update 메서드 호출로 변경 감지
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 내 프로필 조회
     *
     * @param userId 사용자 ID (JWT 토큰에서 추출)
     * @return UserResponse 사용자 정보
     * @throws YamoyoException 사용자를 찾을 수 없는 경우
     */
    public UserResponse getMyProfile(Long userId) {
        log.info("내 프로필 조회 - UserId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.from(user);
    }

    /**
     * 프로필 수정
     *
     * Role:
     * - 전달된 필드만 업데이트 (null이 아닌 필드)
     * - 각 필드는 독립적으로 수정 가능
     *
     * @param userId 사용자 ID (JWT 토큰에서 추출)
     * @param name 변경할 이름 (null이면 변경 안함)
     * @param major 변경할 전공 (null이면 변경 안함)
     * @param mbti 변경할 MBTI (null이면 변경 안함)
     * @param profileImageId 변경할 프로필 이미지 ID (null이면 변경 안함)
     * @return UserResponse 수정된 사용자 정보
     * @throws YamoyoException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public UserResponse updateProfile(Long userId, String name, String major, String mbti, Long profileImageId) {
        log.info("프로필 수정 - UserId: {}, name: {}, major: {}, mbti: {}, profileImageId: {}",
                userId, name, major, mbti, profileImageId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));

        // null이 아닌 필드만 업데이트
        if (name != null) {
            user.updateName(name);
        }
        if (major != null) {
            user.updateMajor(major);
        }
        if (mbti != null) {
            user.updateMBTI(mbti);
        }
        if (profileImageId != null) {
            user.updateProfileImageId(profileImageId);
        }

        log.info("프로필 수정 완료 - UserId: {}", userId);

        return UserResponse.from(user);
    }
}
