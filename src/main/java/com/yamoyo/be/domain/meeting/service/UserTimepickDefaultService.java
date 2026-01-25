package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.repository.UserTimepickDefaultRepository;
import com.yamoyo.be.domain.meeting.util.AvailabilityBitmapConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTimepickDefaultService {

    private final UserTimepickDefaultRepository userTimepickDefaultRepository;

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

}
