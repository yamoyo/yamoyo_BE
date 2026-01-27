package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.dto.response.AvailabilityResponse;
import com.yamoyo.be.domain.meeting.entity.UserTimepickDefault;
import com.yamoyo.be.domain.meeting.entity.WeeklyAvailability;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.repository.UserTimepickDefaultRepository;
import com.yamoyo.be.domain.user.entity.User;
import com.yamoyo.be.domain.user.repository.UserRepository;
import com.yamoyo.be.exception.ErrorCode;
import com.yamoyo.be.exception.YamoyoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserTimepickDefaultService 단위 테스트")
class UserTimepickDefaultServiceTest {

    @Mock
    private UserTimepickDefaultRepository userTimepickDefaultRepository;

    @Mock
    private UserRepository userRepository;

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

    @Nested
    @DisplayName("updateAvailability() - 가용시간 기본값 업데이트")
    class UpdateAvailabilityTest {

        @Test
        @DisplayName("기존 기본값이 있으면 업데이트한다")
        void updateAvailability_ExistingDefault_Updates() {
            // given
            UserTimepickDefault existingDefault = createUserTimepickDefault();
            Map<DayOfWeek, Long> newBitmaps = createBitmaps();

            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(existingDefault));

            // when
            userTimepickDefaultService.updateAvailability(USER_ID, newBitmaps);

            // then
            assertThat(existingDefault.getAvailabilityBitmaps().get(DayOfWeek.MON)).isEqualTo(10L);
            assertThat(existingDefault.getAvailabilityBitmaps().get(DayOfWeek.TUE)).isEqualTo(20L);
        }

        @Test
        @DisplayName("기존 기본값이 없으면 새로 생성한다")
        void updateAvailability_NoDefault_CreatesNew() {
            // given
            User user = createUser();
            Map<DayOfWeek, Long> bitmaps = createBitmaps();

            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());
            given(userRepository.findById(USER_ID))
                    .willReturn(Optional.of(user));

            // when
            userTimepickDefaultService.updateAvailability(USER_ID, bitmaps);

            // then
            ArgumentCaptor<UserTimepickDefault> captor = ArgumentCaptor.forClass(UserTimepickDefault.class);
            verify(userTimepickDefaultRepository).save(captor.capture());

            UserTimepickDefault saved = captor.getValue();
            assertThat(saved.getAvailabilityBitmaps().get(DayOfWeek.MON)).isEqualTo(10L);
            assertThat(saved.getAvailabilityBitmaps().get(DayOfWeek.TUE)).isEqualTo(20L);
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void updateAvailability_UserNotFound() {
            // given
            Map<DayOfWeek, Long> bitmaps = createBitmaps();

            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());
            given(userRepository.findById(USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userTimepickDefaultService.updateAvailability(USER_ID, bitmaps))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("updatePreferredBlock() - 선호시간대 기본값 업데이트")
    class UpdatePreferredBlockTest {

        @Test
        @DisplayName("기존 기본값이 있으면 업데이트한다")
        void updatePreferredBlock_ExistingDefault_Updates() {
            // given
            UserTimepickDefault existingDefault = createUserTimepickDefault();
            PreferredBlock newPreferredBlock = PreferredBlock.BLOCK_16_20;

            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.of(existingDefault));

            // when
            userTimepickDefaultService.updatePreferredBlock(USER_ID, newPreferredBlock);

            // then
            assertThat(existingDefault.getPreferredBlock()).isEqualTo(PreferredBlock.BLOCK_16_20);
        }

        @Test
        @DisplayName("기존 기본값이 없으면 새로 생성한다")
        void updatePreferredBlock_NoDefault_CreatesNew() {
            // given
            User user = createUser();
            PreferredBlock preferredBlock = PreferredBlock.BLOCK_12_16;

            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());
            given(userRepository.findById(USER_ID))
                    .willReturn(Optional.of(user));

            // when
            userTimepickDefaultService.updatePreferredBlock(USER_ID, preferredBlock);

            // then
            ArgumentCaptor<UserTimepickDefault> captor = ArgumentCaptor.forClass(UserTimepickDefault.class);
            verify(userTimepickDefaultRepository).save(captor.capture());

            UserTimepickDefault saved = captor.getValue();
            assertThat(saved.getPreferredBlock()).isEqualTo(PreferredBlock.BLOCK_12_16);
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 USER_NOT_FOUND 예외 발생")
        void updatePreferredBlock_UserNotFound() {
            // given
            PreferredBlock preferredBlock = PreferredBlock.BLOCK_08_12;

            given(userTimepickDefaultRepository.findByUserId(USER_ID))
                    .willReturn(Optional.empty());
            given(userRepository.findById(USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userTimepickDefaultService.updatePreferredBlock(USER_ID, preferredBlock))
                    .isInstanceOf(YamoyoException.class)
                    .satisfies(exception -> {
                        YamoyoException yamoyoException = (YamoyoException) exception;
                        assertThat(yamoyoException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    // ========== Helper Methods ==========

    private UserTimepickDefault createUserTimepickDefault() {
        try {
            var constructor = UserTimepickDefault.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            UserTimepickDefault userDefault = constructor.newInstance();
            ReflectionTestUtils.setField(userDefault, "id", 1L);

            WeeklyAvailability availability = createWeeklyAvailability(1L, 2L, 0L, 0L, 0L, 0L, 0L);
            ReflectionTestUtils.setField(userDefault, "availability", availability);
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

            WeeklyAvailability availability = createWeeklyAvailability(2L, 3L, 4L, 5L, 6L, 7L, 1L);
            ReflectionTestUtils.setField(userDefault, "availability", availability);
            return userDefault;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WeeklyAvailability createWeeklyAvailability(Long mon, Long tue, Long wed, Long thu, Long fri, Long sat, Long sun) {
        try {
            var constructor = WeeklyAvailability.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            WeeklyAvailability availability = constructor.newInstance();
            ReflectionTestUtils.setField(availability, "availabilityMon", mon);
            ReflectionTestUtils.setField(availability, "availabilityTue", tue);
            ReflectionTestUtils.setField(availability, "availabilityWed", wed);
            ReflectionTestUtils.setField(availability, "availabilityThu", thu);
            ReflectionTestUtils.setField(availability, "availabilityFri", fri);
            ReflectionTestUtils.setField(availability, "availabilitySat", sat);
            ReflectionTestUtils.setField(availability, "availabilitySun", sun);
            return availability;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User createUser() {
        try {
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "id", USER_ID);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<DayOfWeek, Long> createBitmaps() {
        Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
        bitmaps.put(DayOfWeek.MON, 10L);
        bitmaps.put(DayOfWeek.TUE, 20L);
        bitmaps.put(DayOfWeek.WED, 0L);
        bitmaps.put(DayOfWeek.THU, 0L);
        bitmaps.put(DayOfWeek.FRI, 0L);
        bitmaps.put(DayOfWeek.SAT, 0L);
        bitmaps.put(DayOfWeek.SUN, 0L);
        return bitmaps;
    }
}
