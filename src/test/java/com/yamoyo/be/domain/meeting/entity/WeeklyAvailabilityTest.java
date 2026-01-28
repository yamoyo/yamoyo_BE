package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeeklyAvailability 단위 테스트")
class WeeklyAvailabilityTest {

    private static final long ALL_AVAILABLE = 0xFFFFFFFFL;

    @Nested
    @DisplayName("allAvailable()")
    class AllAvailableTest {

        @Test
        @DisplayName("모든 요일이 전체 가용으로 생성된다")
        void allAvailable_모든요일_전체가용() {
            // when
            WeeklyAvailability availability = WeeklyAvailability.allAvailable();

            // then
            for (DayOfWeek day : DayOfWeek.values()) {
                assertThat(availability.getFor(day)).isEqualTo(ALL_AVAILABLE);
            }
        }
    }

    @Nested
    @DisplayName("getFor()")
    class GetForTest {

        @Test
        @DisplayName("특정 요일의 비트맵을 직접 반환한다")
        void getFor_특정요일_비트맵반환() {
            // given
            long mondayBitmap = 0b1111L;
            long tuesdayBitmap = 0b11110000L;
            Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
            bitmaps.put(DayOfWeek.MON, mondayBitmap);
            bitmaps.put(DayOfWeek.TUE, tuesdayBitmap);

            WeeklyAvailability availability = WeeklyAvailability.from(bitmaps);

            // when & then
            assertThat(availability.getFor(DayOfWeek.MON)).isEqualTo(mondayBitmap);
            assertThat(availability.getFor(DayOfWeek.TUE)).isEqualTo(tuesdayBitmap);
            assertThat(availability.getFor(DayOfWeek.WED)).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("canAttendOneHourMeetingAt()")
    class CanAttendOneHourMeetingAtTest {

        @Test
        @DisplayName("연속 2슬롯 가용 시 true 반환")
        void canAttendOneHourMeetingAt_연속2슬롯가용_true() {
            // given: 슬롯 4, 5 가용 (10:00~11:00)
            long bitmap = (1L << 4) | (1L << 5);
            Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
            bitmaps.put(DayOfWeek.MON, bitmap);

            WeeklyAvailability availability = WeeklyAvailability.from(bitmaps);

            // when & then
            assertThat(availability.canAttendOneHourMeetingAt(DayOfWeek.MON, 4)).isTrue();
        }

        @Test
        @DisplayName("연속 2슬롯 중 하나만 가용 시 false 반환")
        void canAttendOneHourMeetingAt_1슬롯만가용_false() {
            // given: 슬롯 4만 가용
            long bitmap = (1L << 4);
            Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
            bitmaps.put(DayOfWeek.MON, bitmap);

            WeeklyAvailability availability = WeeklyAvailability.from(bitmaps);

            // when & then
            assertThat(availability.canAttendOneHourMeetingAt(DayOfWeek.MON, 4)).isFalse();
        }

        @Test
        @DisplayName("가용시간 없는 요일은 false 반환")
        void canAttendOneHourMeetingAt_가용시간없음_false() {
            // given: 월요일만 가용
            Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
            bitmaps.put(DayOfWeek.MON, ALL_AVAILABLE);

            WeeklyAvailability availability = WeeklyAvailability.from(bitmaps);

            // when & then
            assertThat(availability.canAttendOneHourMeetingAt(DayOfWeek.TUE, 4)).isFalse();
        }

        @Test
        @DisplayName("전체 가용 시 모든 슬롯 true")
        void canAttendOneHourMeetingAt_전체가용_모든슬롯true() {
            // given
            WeeklyAvailability availability = WeeklyAvailability.allAvailable();

            // when & then: 슬롯 0~30 (31번은 1시간 회의 불가)
            for (int slot = 0; slot < 31; slot++) {
                assertThat(availability.canAttendOneHourMeetingAt(DayOfWeek.MON, slot)).isTrue();
            }
        }
    }
}
