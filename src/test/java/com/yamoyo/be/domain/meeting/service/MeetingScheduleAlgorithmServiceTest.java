package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.WeeklyAvailability;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import com.yamoyo.be.domain.meeting.entity.enums.PreferredBlock;
import com.yamoyo.be.domain.meeting.entity.enums.TimepickParticipantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MeetingScheduleAlgorithmService 단위 테스트")
class MeetingScheduleAlgorithmServiceTest {

    private MeetingScheduleAlgorithmService algorithmService;

    @BeforeEach
    void setUp() {
        algorithmService = new MeetingScheduleAlgorithmService();
    }

    @Nested
    @DisplayName("calculateOptimalSchedule() - 최적 시간 계산")
    class CalculateOptimalScheduleTest {

        @Test
        @DisplayName("전원가능 - 최다 인원 슬롯 선택")
        void calculateOptimalSchedule_전원가능_최다인원슬롯선택() {
            // given: 3명이 월요일 10:00~11:00에만 전원 참석 가능
            // 참가자1: 월요일 10:00~11:00만 가능 (슬롯 4, 5)
            // 참가자2: 월요일 10:00~12:00 가능 (슬롯 4, 5, 6, 7)
            // 참가자3: 월요일 10:00~11:00만 가능 (슬롯 4, 5)
            long mondaySlot_10_11 = (1L << 4) | (1L << 5);
            long mondaySlot_10_12 = (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7);

            TimepickParticipant p1 = createParticipant(
                    createAvailabilityOnMonday(mondaySlot_10_11),
                    PreferredBlock.BLOCK_08_12
            );
            TimepickParticipant p2 = createParticipant(
                    createAvailabilityOnMonday(mondaySlot_10_12),
                    PreferredBlock.BLOCK_08_12
            );
            TimepickParticipant p3 = createParticipant(
                    createAvailabilityOnMonday(mondaySlot_10_11),
                    PreferredBlock.BLOCK_08_12
            );

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2, p3));

            // then: 10:00~11:00가 전원 참석 가능한 유일한 시간
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MON);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(result.availableCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("동일인원 - 선호시간대 우선")
        void calculateOptimalSchedule_동일인원_선호시간대우선() {
            // given: 2명 모두 월요일 전체 가능, 선호시간대가 다름
            // 참가자1: 선호 12~16시
            // 참가자2: 선호 12~16시
            long allAvailable = 0xFFFFFFFFL;

            TimepickParticipant p1 = createParticipant(
                    createAvailabilityOnMonday(allAvailable),
                    PreferredBlock.BLOCK_12_16
            );
            TimepickParticipant p2 = createParticipant(
                    createAvailabilityOnMonday(allAvailable),
                    PreferredBlock.BLOCK_12_16
            );

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2));

            // then: 12:00~13:00가 선호시간대 우선으로 선택됨
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MON);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(12, 0));
            assertThat(result.availableCount()).isEqualTo(2);
            assertThat(result.preferredCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("동일선호 - 월요일 우선")
        void calculateOptimalSchedule_동일선호_월요일우선() {
            // given: 모든 요일 전체 가능, 선호시간대 08~12
            // 월요일이 우선
            Map<DayOfWeek, Long> allDaysAvailable = new EnumMap<>(DayOfWeek.class);
            for (DayOfWeek day : DayOfWeek.values()) {
                allDaysAvailable.put(day, 0xFFFFFFFFL);
            }

            TimepickParticipant p1 = createParticipantWithFullWeek(
                    allDaysAvailable,
                    PreferredBlock.BLOCK_08_12
            );
            TimepickParticipant p2 = createParticipantWithFullWeek(
                    allDaysAvailable,
                    PreferredBlock.BLOCK_08_12
            );

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2));

            // then: 월요일이 우선
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MON);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("동일요일 - 이른 시간 우선")
        void calculateOptimalSchedule_동일요일_이른시간우선() {
            // given: 월요일만 가능, 08:00~12:00 선호
            long morning = 0xFFL; // 슬롯 0~7 (08:00~12:00)

            TimepickParticipant p1 = createParticipant(
                    createAvailabilityOnMonday(morning),
                    PreferredBlock.BLOCK_08_12
            );
            TimepickParticipant p2 = createParticipant(
                    createAvailabilityOnMonday(morning),
                    PreferredBlock.BLOCK_08_12
            );

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2));

            // then: 08:00이 가장 이른 시간
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MON);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(8, 0));
        }

        @Test
        @DisplayName("미응답자 - 전체 가능 처리")
        void calculateOptimalSchedule_미응답자_전체가능처리() {
            // given: 참가자1은 월요일 10시만 가능, 참가자2는 미응답(PENDING)
            long mondaySlot_10_11 = (1L << 4) | (1L << 5);

            TimepickParticipant p1 = createParticipant(
                    createAvailabilityOnMonday(mondaySlot_10_11),
                    PreferredBlock.BLOCK_08_12
            );
            TimepickParticipant p2 = createPendingParticipant();

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2));

            // then: 미응답자는 전체 가능으로 처리되어 2명 참석 가능
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MON);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(result.availableCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("미응답자 선호 - BLOCK_20_24 처리")
        void calculateOptimalSchedule_미응답자선호_BLOCK_20_24처리() {
            // given: 참가자1은 20~24시 선호, 참가자2는 미응답
            long evening = 0xFF000000L; // 슬롯 24~31 (20:00~24:00)

            TimepickParticipant p1 = createParticipant(
                    createAvailabilityOnMonday(evening),
                    PreferredBlock.BLOCK_20_24
            );
            TimepickParticipant p2 = createPendingParticipant();

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2));

            // then: 미응답자는 20~24시 선호로 처리, 둘 다 선호 시간대
            assertThat(result.preferredCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("서로 다른 가용시간 - 겹치는 시간대 선택")
        void calculateOptimalSchedule_서로다른가용시간_겹치는시간대선택() {
            // given
            // 참가자1: 09:00~12:00 (슬롯 2~7)
            // 참가자2: 10:00~13:00 (슬롯 4~9)
            // 겹치는 시간: 10:00~12:00 (슬롯 4~7)
            long slot_09_12 = (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7);
            long slot_10_13 = (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9);

            TimepickParticipant p1 = createParticipant(
                    createAvailabilityOnMonday(slot_09_12),
                    PreferredBlock.BLOCK_08_12
            );
            TimepickParticipant p2 = createParticipant(
                    createAvailabilityOnMonday(slot_10_13),
                    PreferredBlock.BLOCK_08_12
            );

            // when
            MeetingScheduleAlgorithmService.ScheduleResult result =
                    algorithmService.calculateOptimalSchedule(List.of(p1, p2));

            // then: 10:00~11:00가 가장 이른 겹치는 시간
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MON);
            assertThat(result.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(result.availableCount()).isEqualTo(2);
        }
    }

    // ========== Helper Methods ==========

    private WeeklyAvailability createAvailabilityOnMonday(long mondayBitmap) {
        Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
        bitmaps.put(DayOfWeek.MON, mondayBitmap);
        return WeeklyAvailability.from(bitmaps);
    }

    private TimepickParticipant createParticipant(WeeklyAvailability availability, PreferredBlock preferredBlock) {
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

    private TimepickParticipant createParticipantWithFullWeek(Map<DayOfWeek, Long> bitmaps, PreferredBlock preferredBlock) {
        try {
            var constructor = TimepickParticipant.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            TimepickParticipant participant = constructor.newInstance();
            ReflectionTestUtils.setField(participant, "id", 1L);
            ReflectionTestUtils.setField(participant, "availability", WeeklyAvailability.from(bitmaps));
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
            ReflectionTestUtils.setField(participant, "id", 2L);
            ReflectionTestUtils.setField(participant, "availability", WeeklyAvailability.empty());
            ReflectionTestUtils.setField(participant, "preferredBlock", null);
            ReflectionTestUtils.setField(participant, "availabilityStatus", TimepickParticipantStatus.PENDING);
            ReflectionTestUtils.setField(participant, "preferredBlockStatus", TimepickParticipantStatus.PENDING);
            return participant;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
