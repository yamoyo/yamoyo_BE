package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickParticipantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimepickParticipant 단위 테스트")
class TimepickParticipantTest {

    private static final long ALL_AVAILABLE = 0xFFFFFFFFL;

    @Nested
    @DisplayName("getEffectiveAvailabilityFor()")
    class GetEffectiveAvailabilityForTest {

        @Test
        @DisplayName("제출된 경우 실제 가용시간 반환")
        void getEffectiveAvailabilityFor_제출_실제값반환() {
            // given
            long mondayBitmap = (1L << 4) | (1L << 5);
            TimepickParticipant participant = createSubmittedParticipant(
                    createAvailabilityOnMonday(mondayBitmap),
                    PreferredBlock.BLOCK_08_12
            );

            // when & then
            assertThat(participant.getEffectiveAvailabilityFor(DayOfWeek.MON)).isEqualTo(mondayBitmap);
        }

        @Test
        @DisplayName("PENDING 상태면 전체 가용 반환")
        void getEffectiveAvailabilityFor_PENDING_전체가용반환() {
            // given
            TimepickParticipant participant = createPendingParticipant();

            // when & then
            assertThat(participant.getEffectiveAvailabilityFor(DayOfWeek.MON)).isEqualTo(ALL_AVAILABLE);
        }

        @Test
        @DisplayName("EXPIRED 상태면 전체 가용 반환")
        void getEffectiveAvailabilityFor_EXPIRED_전체가용반환() {
            // given
            TimepickParticipant participant = createExpiredParticipant();

            // when & then
            assertThat(participant.getEffectiveAvailabilityFor(DayOfWeek.MON)).isEqualTo(ALL_AVAILABLE);
        }
    }

    @Nested
    @DisplayName("getEffectivePreferredBlock()")
    class GetEffectivePreferredBlockTest {

        @Test
        @DisplayName("제출된 경우 실제 선호시간대 반환")
        void getEffectivePreferredBlock_제출_실제값반환() {
            // given
            TimepickParticipant participant = createSubmittedParticipant(
                    WeeklyAvailability.empty(),
                    PreferredBlock.BLOCK_08_12
            );

            // when & then
            assertThat(participant.getEffectivePreferredBlock()).isEqualTo(PreferredBlock.BLOCK_08_12);
        }

        @Test
        @DisplayName("PENDING 상태면 BLOCK_20_24 반환")
        void getEffectivePreferredBlock_PENDING_기본값반환() {
            // given
            TimepickParticipant participant = createPendingParticipant();

            // when & then
            assertThat(participant.getEffectivePreferredBlock()).isEqualTo(PreferredBlock.BLOCK_20_24);
        }

        @Test
        @DisplayName("제출했지만 null이면 BLOCK_20_24 반환")
        void getEffectivePreferredBlock_제출했지만null_기본값반환() {
            // given
            TimepickParticipant participant = createSubmittedParticipant(
                    WeeklyAvailability.empty(),
                    null
            );

            // when & then
            assertThat(participant.getEffectivePreferredBlock()).isEqualTo(PreferredBlock.BLOCK_20_24);
        }
    }

    @Nested
    @DisplayName("canAttendOneHourMeetingAt()")
    class CanAttendOneHourMeetingAtTest {

        @Test
        @DisplayName("제출된 경우 실제 가용시간 기준으로 판단")
        void canAttendOneHourMeetingAt_제출_실제값기준() {
            // given: 월요일 10:00~11:00만 가능 (슬롯 4, 5)
            long mondayBitmap = (1L << 4) | (1L << 5);
            TimepickParticipant participant = createSubmittedParticipant(
                    createAvailabilityOnMonday(mondayBitmap),
                    PreferredBlock.BLOCK_08_12
            );

            // when & then
            assertThat(participant.canAttendOneHourMeetingAt(DayOfWeek.MON, 4)).isTrue();
            assertThat(participant.canAttendOneHourMeetingAt(DayOfWeek.MON, 3)).isFalse();
            assertThat(participant.canAttendOneHourMeetingAt(DayOfWeek.MON, 5)).isFalse();
        }

        @Test
        @DisplayName("PENDING 상태면 항상 true")
        void canAttendOneHourMeetingAt_PENDING_항상true() {
            // given
            TimepickParticipant participant = createPendingParticipant();

            // when & then
            for (int slot = 0; slot < 31; slot++) {
                assertThat(participant.canAttendOneHourMeetingAt(DayOfWeek.MON, slot)).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("prefersSlot()")
    class PrefersSlotTest {

        @Test
        @DisplayName("선호 시간대에 포함된 슬롯은 true")
        void prefersSlot_선호시간대포함_true() {
            // given: BLOCK_08_12 선호 (슬롯 0~7)
            TimepickParticipant participant = createSubmittedParticipant(
                    WeeklyAvailability.empty(),
                    PreferredBlock.BLOCK_08_12
            );

            // when & then: 슬롯 0~6은 선호 (1시간 회의가 블록 내 완전 포함)
            assertThat(participant.prefersSlot(0)).isTrue();
            assertThat(participant.prefersSlot(6)).isTrue();
            assertThat(participant.prefersSlot(7)).isFalse(); // 1시간 회의가 블록 벗어남
        }

        @Test
        @DisplayName("PENDING 상태면 BLOCK_20_24 기준")
        void prefersSlot_PENDING_기본값기준() {
            // given
            TimepickParticipant participant = createPendingParticipant();

            // when & then: BLOCK_20_24 (슬롯 24~31)
            assertThat(participant.prefersSlot(24)).isTrue();
            assertThat(participant.prefersSlot(30)).isTrue();
            assertThat(participant.prefersSlot(23)).isFalse();
        }
    }

    // ========== Helper Methods ==========

    private WeeklyAvailability createAvailabilityOnMonday(long mondayBitmap) {
        Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
        bitmaps.put(DayOfWeek.MON, mondayBitmap);
        return WeeklyAvailability.from(bitmaps);
    }

    private TimepickParticipant createSubmittedParticipant(WeeklyAvailability availability, PreferredBlock preferredBlock) {
        try {
            var constructor = TimepickParticipant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TimepickParticipant participant = constructor.newInstance();
            ReflectionTestUtils.setField(participant, "id", 1L);
            ReflectionTestUtils.setField(participant, "availability", availability);
            ReflectionTestUtils.setField(participant, "preferredBlock", preferredBlock);
            ReflectionTestUtils.setField(participant, "availabilityStatus", TimepickParticipantStatus.SUBMITTED);
            ReflectionTestUtils.setField(participant, "preferredBlockStatus", TimepickParticipantStatus.SUBMITTED);
            return participant;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TimepickParticipant createPendingParticipant() {
        try {
            var constructor = TimepickParticipant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TimepickParticipant participant = constructor.newInstance();
            ReflectionTestUtils.setField(participant, "id", 1L);
            ReflectionTestUtils.setField(participant, "availability", WeeklyAvailability.empty());
            ReflectionTestUtils.setField(participant, "preferredBlock", null);
            ReflectionTestUtils.setField(participant, "availabilityStatus", TimepickParticipantStatus.PENDING);
            ReflectionTestUtils.setField(participant, "preferredBlockStatus", TimepickParticipantStatus.PENDING);
            return participant;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TimepickParticipant createExpiredParticipant() {
        try {
            var constructor = TimepickParticipant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TimepickParticipant participant = constructor.newInstance();
            ReflectionTestUtils.setField(participant, "id", 1L);
            ReflectionTestUtils.setField(participant, "availability", WeeklyAvailability.empty());
            ReflectionTestUtils.setField(participant, "preferredBlock", null);
            ReflectionTestUtils.setField(participant, "availabilityStatus", TimepickParticipantStatus.EXPIRED);
            ReflectionTestUtils.setField(participant, "preferredBlockStatus", TimepickParticipantStatus.EXPIRED);
            return participant;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
