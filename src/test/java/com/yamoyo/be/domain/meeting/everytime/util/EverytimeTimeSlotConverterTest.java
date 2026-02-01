package com.yamoyo.be.domain.meeting.everytime.util;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EverytimeTimeSlotConverter 단위 테스트")
class EverytimeTimeSlotConverterTest {

    @Nested
    @DisplayName("toDayOfWeek() - 경계값/예외만 테스트")
    class ToDayOfWeekTest {

        @Test
        @DisplayName("day 0은 월요일 (하한 경계)")
        void day0_Monday() {
            assertThat(EverytimeTimeSlotConverter.toDayOfWeek(0)).isEqualTo(DayOfWeek.MON);
        }

        @Test
        @DisplayName("day 4는 금요일 (상한 경계)")
        void day4_Friday() {
            assertThat(EverytimeTimeSlotConverter.toDayOfWeek(4)).isEqualTo(DayOfWeek.FRI);
        }

        @Test
        @DisplayName("day -1은 예외 (하한 초과)")
        void negativeDay_ThrowsException() {
            assertThatThrownBy(() -> EverytimeTimeSlotConverter.toDayOfWeek(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid day");
        }

        @Test
        @DisplayName("day 5는 예외 (상한 초과)")
        void day5_ThrowsException() {
            assertThatThrownBy(() -> EverytimeTimeSlotConverter.toDayOfWeek(5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid day");
        }
    }

    @Nested
    @DisplayName("toSlotIndex() - 08:00 기준 경계값 테스트")
    class ToSlotIndexTest {

        @Test
        @DisplayName("479분(07:59)은 -1 (범위 미만)")
        void minute479_outOfRange() {
            assertThat(EverytimeTimeSlotConverter.toSlotIndex(479)).isEqualTo(-1);
        }

        @Test
        @DisplayName("480분(08:00)은 슬롯 0")
        void minute480_slot0() {
            assertThat(EverytimeTimeSlotConverter.toSlotIndex(480)).isEqualTo(0);
        }

        @Test
        @DisplayName("509분(08:29)은 슬롯 0")
        void minute509_slot0() {
            assertThat(EverytimeTimeSlotConverter.toSlotIndex(509)).isEqualTo(0);
        }

        @Test
        @DisplayName("510분(08:30)은 슬롯 1")
        void minute510_slot1() {
            assertThat(EverytimeTimeSlotConverter.toSlotIndex(510)).isEqualTo(1);
        }

        @Test
        @DisplayName("1439분(23:59)은 슬롯 31")
        void minute1439_slot31() {
            assertThat(EverytimeTimeSlotConverter.toSlotIndex(1439)).isEqualTo(31);
        }

        @Test
        @DisplayName("1440분(24:00)은 -1 (범위 초과)")
        void minute1440_outOfRange() {
            assertThat(EverytimeTimeSlotConverter.toSlotIndex(1440)).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("markBusy() - 08:00 기준 슬롯 경계 처리 핵심 케이스")
    class MarkBusyTest {

        private boolean[] createAllTrueSlots() {
            boolean[] slots = new boolean[32];
            for (int i = 0; i < 32; i++) {
                slots[i] = true;
            }
            return slots;
        }

        @Test
        @DisplayName("슬롯 중간에서 끝남 (10:10 종료) → 10:00~10:30 슬롯 불가")
        void endInMiddleOfSlot_marksSlotBusy() {
            boolean[] slots = createAllTrueSlots();

            // 09:30~10:10 수업 (570분~610분)
            EverytimeTimeSlotConverter.markBusy(slots, 570, 610);

            assertThat(slots[2]).isTrue();   // 09:00~09:30 가능
            assertThat(slots[3]).isFalse();  // 09:30~10:00 불가 (수업 시작)
            assertThat(slots[4]).isFalse();  // 10:00~10:30 불가 (10:10에 끝나므로 슬롯 차지)
            assertThat(slots[5]).isTrue();   // 10:30~11:00 가능
        }

        @Test
        @DisplayName("슬롯 경계에서 정확히 끝남 (10:00 종료) → 10:00~10:30 슬롯 가능")
        void endExactlyAtBoundary_nextSlotAvailable() {
            boolean[] slots = createAllTrueSlots();

            // 09:30~10:00 수업 (570분~600분)
            EverytimeTimeSlotConverter.markBusy(slots, 570, 600);

            assertThat(slots[2]).isTrue();   // 09:00~09:30 가능
            assertThat(slots[3]).isFalse();  // 09:30~10:00 불가
            assertThat(slots[4]).isTrue();   // 10:00~10:30 가능 (경계에서 끝남)
        }

        @Test
        @DisplayName("슬롯 중간에서 시작 (10:10 시작) → 10:00~10:30 슬롯 불가")
        void startInMiddleOfSlot_marksSlotBusy() {
            boolean[] slots = createAllTrueSlots();

            // 10:10~10:40 수업 (610분~640분)
            EverytimeTimeSlotConverter.markBusy(slots, 610, 640);

            assertThat(slots[3]).isTrue();   // 09:30~10:00 가능
            assertThat(slots[4]).isFalse();  // 10:00~10:30 불가 (10:10에 시작)
            assertThat(slots[5]).isFalse();  // 10:30~11:00 불가 (10:40에 끝남)
            assertThat(slots[6]).isTrue();   // 11:00~11:30 가능
        }

        @Test
        @DisplayName("슬롯 경계에서 정확히 시작 (10:00 시작) → 09:30~10:00 슬롯 가능")
        void startExactlyAtBoundary_previousSlotAvailable() {
            boolean[] slots = createAllTrueSlots();

            // 10:00~10:30 수업 (600분~630분)
            EverytimeTimeSlotConverter.markBusy(slots, 600, 630);

            assertThat(slots[3]).isTrue();   // 09:30~10:00 가능 (경계에서 시작)
            assertThat(slots[4]).isFalse();  // 10:00~10:30 불가
            assertThat(slots[5]).isTrue();   // 10:30~11:00 가능
        }

        @Test
        @DisplayName("1분짜리 수업 (10:00~10:01) → 해당 슬롯만 불가")
        void oneMinuteClass_marksOnlyOneSlot() {
            boolean[] slots = createAllTrueSlots();

            // 10:00~10:01 수업 (600분~601분)
            EverytimeTimeSlotConverter.markBusy(slots, 600, 601);

            assertThat(slots[3]).isTrue();   // 09:30~10:00 가능
            assertThat(slots[4]).isFalse();  // 10:00~10:30 불가
            assertThat(slots[5]).isTrue();   // 10:30~11:00 가능
        }

        @Test
        @DisplayName("연속 수업 (09:00~10:00, 10:00~11:00) → 경계 슬롯 정확히 처리")
        void consecutiveClasses_boundaryHandledCorrectly() {
            boolean[] slots = createAllTrueSlots();

            // 첫 번째 수업: 09:00~10:00 (540분~600분)
            EverytimeTimeSlotConverter.markBusy(slots, 540, 600);
            // 두 번째 수업: 10:00~11:00 (600분~660분)
            EverytimeTimeSlotConverter.markBusy(slots, 600, 660);

            assertThat(slots[1]).isTrue();   // 08:30~09:00 가능
            assertThat(slots[2]).isFalse();  // 09:00~09:30 불가 (첫 수업)
            assertThat(slots[3]).isFalse();  // 09:30~10:00 불가 (첫 수업)
            assertThat(slots[4]).isFalse();  // 10:00~10:30 불가 (두번째 수업)
            assertThat(slots[5]).isFalse();  // 10:30~11:00 불가 (두번째 수업)
            assertThat(slots[6]).isTrue();   // 11:00~11:30 가능
        }

        @Test
        @DisplayName("08:00 이전 시작 시간은 슬롯 0부터 마킹")
        void earlyStart_startsFromSlot0() {
            boolean[] slots = createAllTrueSlots();

            // 07:00~09:00 수업 (420분~540분)
            EverytimeTimeSlotConverter.markBusy(slots, 420, 540);

            assertThat(slots[0]).isFalse();  // 08:00~08:30 불가
            assertThat(slots[1]).isFalse();  // 08:30~09:00 불가
            assertThat(slots[2]).isTrue();   // 09:00~09:30 가능
        }
    }

    @Nested
    @DisplayName("createFullAvailabilityMap() - 기본 검증")
    class CreateFullAvailabilityMapTest {

        @Test
        @DisplayName("7개 요일, 각 32슬롯 모두 true로 초기화")
        void createsMapWithAllDaysAndTrueSlots() {
            Map<DayOfWeek, boolean[]> map = EverytimeTimeSlotConverter.createFullAvailabilityMap();

            assertThat(map).hasSize(7);
            assertThat(map).containsKeys(
                    DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED,
                    DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT, DayOfWeek.SUN
            );

            for (DayOfWeek day : DayOfWeek.values()) {
                boolean[] slots = map.get(day);
                assertThat(slots).hasSize(32);
                assertThat(slots).containsOnly(true);
            }
        }
    }

    @Nested
    @DisplayName("통합 시나리오")
    class IntegrationTest {

        @Test
        @DisplayName("Everytime 수업 시간을 타임픽 슬롯으로 변환")
        void convertEverytimeClassToSlots() {
            // given: Everytime 수업 - 월요일 09:00~10:30
            int day = 0;
            int starttime = 108;  // 09:00 = 540분
            int endtime = 126;    // 10:30 = 630분

            Map<DayOfWeek, boolean[]> availability = EverytimeTimeSlotConverter.createFullAvailabilityMap();

            // when
            DayOfWeek dayOfWeek = EverytimeTimeSlotConverter.toDayOfWeek(day);
            int startMinutes = EverytimeTimeSlotConverter.toMinutes(starttime);
            int endMinutes = EverytimeTimeSlotConverter.toMinutes(endtime);

            boolean[] slots = availability.get(dayOfWeek);
            EverytimeTimeSlotConverter.markBusy(slots, startMinutes, endMinutes);

            // then: 08:00 기준으로 슬롯[2]=09:00, 슬롯[3]=09:30, 슬롯[4]=10:00
            assertThat(dayOfWeek).isEqualTo(DayOfWeek.MON);
            assertThat(slots[1]).isTrue();   // 08:30~09:00 가능
            assertThat(slots[2]).isFalse();  // 09:00~09:30 불가
            assertThat(slots[3]).isFalse();  // 09:30~10:00 불가
            assertThat(slots[4]).isFalse();  // 10:00~10:30 불가
            assertThat(slots[5]).isTrue();   // 10:30~11:00 가능
        }
    }
}
