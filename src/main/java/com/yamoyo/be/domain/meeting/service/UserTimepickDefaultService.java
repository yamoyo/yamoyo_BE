package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.entity.UserTimepickDefault;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.repository.UserTimepickDefaultRepository;
import com.yamoyo.be.domain.meeting.util.AvailabilityBitmapConverter;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTimepickDefaultService {

    private final UserTimepickDefaultRepository userTimepickDefaultRepository;
    private final UserRepository userRepository;

    public AvailabilityResponse getAvailability(Long userId) {
        return userTimepickDefaultRepository.findByUserId(userId)
                .map(userDefault -> {
                    Map<DayOfWeek, Long> bitmaps = userDefault.getAvailabilityBitmaps();
                    Map<DayOfWeek, boolean[]> availability = AvailabilityBitmapConverter.toBooleanArrays(bitmaps);
                    log.debug("사용자 기본 가용시간 조회 - UserId: {}", userId);
                    return AvailabilityResponse.from(availability);
                })
                .orElseGet(() -> {
                    log.debug("사용자 기본 가용시간 없음 - UserId: {}, 빈 가용시간 반환", userId);
                    return AvailabilityResponse.from(AvailabilityBitmapConverter.createEmptyAvailability());
                });
    }

    @Transactional
    public void updateAvailability(Long userId, Map<DayOfWeek, Long> bitmaps) {
        upsertDefault(
                userId,
                userDefault -> userDefault.updateAvailability(bitmaps),
                user -> UserTimepickDefault.createWithAvailability(user, bitmaps)
        );
    }

    @Transactional
    public void updatePreferredBlock(Long userId, PreferredBlock preferredBlock) {
        upsertDefault(
                userId,
                userDefault -> userDefault.updatePreferredBlock(preferredBlock),
                user -> UserTimepickDefault.createWithPreferredBlock(user, preferredBlock)
        );
    }

    private void upsertDefault(Long userId,
                               Consumer<UserTimepickDefault> updater,
                               Function<User, UserTimepickDefault> creator) {
        userTimepickDefaultRepository.findByUserId(userId)
                .ifPresentOrElse(
                        updater,
                        () -> {
                            User user = userRepository.findById(userId)
                                    .orElseThrow(() -> new YamoyoException(ErrorCode.USER_NOT_FOUND));
                            userTimepickDefaultRepository.save(creator.apply(user));
                        }
                );
    }
}
