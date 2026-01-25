package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.entity.UserTimepickDefault;
import com.yamoyo.be.domain.meeting.repository.UserTimepickDefaultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserTimepickDefaultService 단위 테스트")
class UserTimepickDefaultServiceTest {

    @Mock
    private UserTimepickDefaultRepository userTimepickDefaultRepository;

    @InjectMocks
    private UserTimepickDefaultService userTimepickDefaultService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("getAvailability() - 가용시간 기본값 조회")
    class GetAvailabilityTest {

        @Test
        @DisplayName("사용자 기본값이 없으면 모두 false인 가용시간 반환")
        void noDefault_ReturnsAllFalse() {
            // given
            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());

            // when
            AvailabilityResponse response = userTimepickDefaultService.getAvailability(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.availability()).isNotNull();
            assertThat(response.availability().sunday()).hasSize(32);
            assertThat(response.availability().sunday()).containsOnly(false);
            assertThat(response.availability().monday()).containsOnly(false);
            assertThat(response.availability().tuesday()).containsOnly(false);
            assertThat(response.availability().wednesday()).containsOnly(false);
            assertThat(response.availability().thursday()).containsOnly(false);
            assertThat(response.availability().friday()).containsOnly(false);
            assertThat(response.availability().saturday()).containsOnly(false);
        }

        @Test
        @DisplayName("사용자 기본값이 있으면 비트맵을 boolean 배열로 변환하여 반환")
        void hasDefault_ReturnsBitmapAsBoolean() {
            // given
            UserTimepickDefault userDefault = createUserTimepickDefault();
            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(userDefault));

            // when
            AvailabilityResponse response = userTimepickDefaultService.getAvailability(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.availability()).isNotNull();
            // 월요일 비트맵 1 → 첫 번째 슬롯만 true
            assertThat(response.availability().monday()[0]).isTrue();
            assertThat(response.availability().monday()[1]).isFalse();
            // 화요일 비트맵 2 → 두 번째 슬롯만 true
            assertThat(response.availability().tuesday()[0]).isFalse();
            assertThat(response.availability().tuesday()[1]).isTrue();
            // 나머지는 모두 false
            assertThat(response.availability().wednesday()).containsOnly(false);
        }

        @Test
        @DisplayName("모든 요일의 가용시간이 정상적으로 변환됨")
        void allDaysConverted() {
            // given
            UserTimepickDefault userDefault = createUserTimepickDefaultAllDays();
            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(userDefault));

            // when
            AvailabilityResponse response = userTimepickDefaultService.getAvailability(USER_ID);

            // then
            assertThat(response.availability().sunday()[0]).isTrue();   // 비트맵 1
            assertThat(response.availability().monday()[1]).isTrue();   // 비트맵 2
            assertThat(response.availability().tuesday()[0]).isTrue();  // 비트맵 3
            assertThat(response.availability().tuesday()[1]).isTrue();  // 비트맵 3
            assertThat(response.availability().wednesday()[2]).isTrue(); // 비트맵 4
            assertThat(response.availability().thursday()[0]).isTrue(); // 비트맵 5
            assertThat(response.availability().thursday()[2]).isTrue(); // 비트맵 5
            assertThat(response.availability().friday()[1]).isTrue();   // 비트맵 6
            assertThat(response.availability().friday()[2]).isTrue();   // 비트맵 6
            assertThat(response.availability().saturday()[0]).isTrue(); // 비트맵 7
            assertThat(response.availability().saturday()[1]).isTrue(); // 비트맵 7
            assertThat(response.availability().saturday()[2]).isTrue(); // 비트맵 7
        }
    }

    // ========== Helper Methods ==========

    private UserTimepickDefault createUserTimepickDefault() {
        try {
            var constructor = UserTimepickDefault.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            UserTimepickDefault userDefault = constructor.newInstance();
            ReflectionTestUtils.setField(userDefault, "id", 1L);
            ReflectionTestUtils.setField(userDefault, "availabilityMon", 1L);  // 첫 번째 슬롯만 true
            ReflectionTestUtils.setField(userDefault, "availabilityTue", 2L);  // 두 번째 슬롯만 true
            ReflectionTestUtils.setField(userDefault, "availabilityWed", 0L);
            ReflectionTestUtils.setField(userDefault, "availabilityThu", 0L);
            ReflectionTestUtils.setField(userDefault, "availabilityFri", 0L);
            ReflectionTestUtils.setField(userDefault, "availabilitySat", 0L);
            ReflectionTestUtils.setField(userDefault, "availabilitySun", 0L);
            return userDefault;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserTimepickDefault createUserTimepickDefaultAllDays() {
        try {
            var constructor = UserTimepickDefault.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            UserTimepickDefault userDefault = constructor.newInstance();
            ReflectionTestUtils.setField(userDefault, "id", 1L);
            ReflectionTestUtils.setField(userDefault, "availabilitySun", 1L);  // 0b001
            ReflectionTestUtils.setField(userDefault, "availabilityMon", 2L);  // 0b010
            ReflectionTestUtils.setField(userDefault, "availabilityTue", 3L);  // 0b011
            ReflectionTestUtils.setField(userDefault, "availabilityWed", 4L);  // 0b100
            ReflectionTestUtils.setField(userDefault, "availabilityThu", 5L);  // 0b101
            ReflectionTestUtils.setField(userDefault, "availabilityFri", 6L);  // 0b110
            ReflectionTestUtils.setField(userDefault, "availabilitySat", 7L);  // 0b111
            return userDefault;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
