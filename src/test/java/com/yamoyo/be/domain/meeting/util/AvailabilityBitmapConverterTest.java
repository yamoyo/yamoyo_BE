package com.yamoyo.be.domain.meeting.util;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AvailabilityBitmapConverter 단위 테스트")
class AvailabilityBitmapConverterTest {

    @Nested
    @DisplayName("toBooleanArray() - 단일 비트맵 → boolean[32] 변환")
    class ToBooleanArrayTest {

        @Test
        @DisplayName("비트맵 0은 모두 false인 배열 반환")
        void zeroBitmap_AllFalse() {
            // given: 비트맵 0 = 가용시간 없음
            // 이진수: 0b00000000...

            // when
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(0L);

            // then: 32개 슬롯 모두 false (08:00~24:00 전부 불가)
            assertThat(result).hasSize(32);
            assertThat(result).containsOnly(false);
        }

        @Test
        @DisplayName("비트맵 1은 첫 번째 슬롯만 true")
        void bitmapOne_FirstSlotTrue() {
            // given: 비트맵 1 = 2^0 = 슬롯[0]만 true
            // 이진수: 0b00000001
            // 의미: 08:00~08:30만 가능

            // when
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(1L);

            // then: 슬롯[0]=true (08:00~08:30), 나머지 false
            assertThat(result).hasSize(32);
            assertThat(result[0]).isTrue();
            for (int i = 1; i < 32; i++) {
                assertThat(result[i]).isFalse();
            }
        }

        @Test
        @DisplayName("비트맵 2는 두 번째 슬롯만 true")
        void bitmapTwo_SecondSlotTrue() {
            // given: 비트맵 2 = 2^1 = 슬롯[1]만 true
            // 이진수: 0b00000010
            // 의미: 08:30~09:00만 가능

            // when
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(2L);

            // then: 슬롯[0]=false, 슬롯[1]=true
            assertThat(result[0]).isFalse();  // 08:00~08:30 불가
            assertThat(result[1]).isTrue();   // 08:30~09:00 가능
        }

        @Test
        @DisplayName("비트맵 3은 첫 번째와 두 번째 슬롯이 true")
        void bitmapThree_FirstTwoSlotsTrue() {
            // given: 비트맵 3 = 2^0 + 2^1 = 1 + 2 = 슬롯[0,1] true
            // 이진수: 0b00000011
            // 의미: 08:00~09:00 (1시간 연속) 가능

            // when
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(3L);

            // then: 슬롯[0,1]=true, 슬롯[2]=false
            assertThat(result[0]).isTrue();   // 08:00~08:30 가능
            assertThat(result[1]).isTrue();   // 08:30~09:00 가능
            assertThat(result[2]).isFalse();  // 09:00~09:30 불가
        }

        @Test
        @DisplayName("null 비트맵은 모두 false인 배열 반환")
        void nullBitmap_AllFalse() {
            // when
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(null);

            // then
            assertThat(result).hasSize(32);
            assertThat(result).containsOnly(false);
        }

        @Test
        @DisplayName("전체 비트가 1인 경우 모두 true")
        void allBitsSet_AllTrue() {
            // given: 0xFFFFFFFF = 32비트 모두 1
            // 이진수: 0b11111111111111111111111111111111
            // 의미: 08:00~24:00 종일 가능
            long allOnes = 0xFFFFFFFFL;

            // when
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(allOnes);

            // then: 32개 슬롯 모두 true (종일 가능)
            assertThat(result).hasSize(32);
            assertThat(result).containsOnly(true);
        }
    }

    @Nested
    @DisplayName("toBitmap() - boolean[32] → 비트맵 변환")
    class ToBitmapTest {

        @Test
        @DisplayName("모두 false인 배열은 0 반환")
        void allFalse_ZeroBitmap() {
            // given: 32개 슬롯 모두 false (가용시간 없음)
            boolean[] array = new boolean[32];

            // when
            Long result = AvailabilityBitmapConverter.toBitmap(array);

            // then: 비트맵 0 (모든 비트가 0)
            assertThat(result).isEqualTo(0L);
        }

        @Test
        @DisplayName("첫 번째 슬롯만 true면 1 반환")
        void firstSlotTrue_BitmapOne() {
            // given: 슬롯[0]=true (08:00~08:30만 가능)
            boolean[] array = new boolean[32];
            array[0] = true;

            // when
            Long result = AvailabilityBitmapConverter.toBitmap(array);

            // then: 비트맵 1 = 2^0
            // 비트 연산: 0 | (1 << 0) = 0 | 1 = 1
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("두 번째 슬롯만 true면 2 반환")
        void secondSlotTrue_BitmapTwo() {
            // given: 슬롯[1]=true (08:30~09:00만 가능)
            boolean[] array = new boolean[32];
            array[1] = true;

            // when
            Long result = AvailabilityBitmapConverter.toBitmap(array);

            // then: 비트맵 2 = 2^1
            // 비트 연산: 0 | (1 << 1) = 0 | 2 = 2
            assertThat(result).isEqualTo(2L);
        }

        @Test
        @DisplayName("null 배열은 0 반환")
        void nullArray_ZeroBitmap() {
            // when
            Long result = AvailabilityBitmapConverter.toBitmap(null);

            // then
            assertThat(result).isEqualTo(0L);
        }

        @Test
        @DisplayName("빈 배열은 0 반환")
        void emptyArray_ZeroBitmap() {
            // given
            boolean[] array = new boolean[0];

            // when
            Long result = AvailabilityBitmapConverter.toBitmap(array);

            // then
            assertThat(result).isEqualTo(0L);
        }

        @Test
        @DisplayName("모두 true인 배열은 모든 비트가 1인 값 반환")
        void allTrue_AllBitsSet() {
            // given: 32개 슬롯 모두 true (08:00~24:00 종일 가능)
            boolean[] array = new boolean[32];
            for (int i = 0; i < 32; i++) {
                array[i] = true;
            }

            // when
            Long result = AvailabilityBitmapConverter.toBitmap(array);

            // then: 0xFFFFFFFF = 4294967295 (32비트 모두 1)
            // 비트 연산: 2^0 + 2^1 + ... + 2^31 = 4294967295
            assertThat(result).isEqualTo(0xFFFFFFFFL);
        }
    }

    @Nested
    @DisplayName("toBooleanArray ↔ toBitmap 왕복 변환")
    class RoundTripTest {

        @Test
        @DisplayName("비트맵 → boolean[] → 비트맵 왕복 변환 일치")
        void bitmapRoundTrip() {
            // given: 짝수 슬롯만 true인 패턴 (08:30~09:00, 09:30~10:00, ...)
            // 이진수: 10101010...10 (짝수 비트만 1)
            // 의미: 30분 간격으로 번갈아가며 가능
            long original = 0b10101010101010101010101010101010L;

            // when: 비트맵 → boolean[] → 비트맵
            boolean[] array = AvailabilityBitmapConverter.toBooleanArray(original);
            Long result = AvailabilityBitmapConverter.toBitmap(array);

            // then: 왕복 변환 후 원본과 일치
            assertThat(result).isEqualTo(original);
        }

        @Test
        @DisplayName("boolean[] → 비트맵 → boolean[] 왕복 변환 일치")
        void booleanArrayRoundTrip() {
            // given: 특정 시간대만 가능한 케이스
            // 슬롯[0]  = 08:00~08:30 가능
            // 슬롯[5]  = 10:30~11:00 가능
            // 슬롯[10] = 13:00~13:30 가능
            // 슬롯[31] = 23:30~24:00 가능
            boolean[] original = new boolean[32];
            original[0] = true;   // 08:00~08:30
            original[5] = true;   // 10:30~11:00
            original[10] = true;  // 13:00~13:30
            original[31] = true;  // 23:30~24:00

            // when: boolean[] → 비트맵 → boolean[]
            // 예상 비트맵: 2^0 + 2^5 + 2^10 + 2^31 = 1 + 32 + 1024 + 2147483648
            Long bitmap = AvailabilityBitmapConverter.toBitmap(original);
            boolean[] result = AvailabilityBitmapConverter.toBooleanArray(bitmap);

            // then: 왕복 변환 후 원본과 일치
            assertThat(result).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("toBooleanArrays() - 요일별 비트맵 맵 변환")
    class ToBooleanArraysTest {

        @Test
        @DisplayName("요일별 비트맵을 boolean 배열 맵으로 변환")
        void convertDayOfWeekMap() {
            // given: 요일별로 다른 가용시간 설정
            // 월요일: 비트맵 1 = 08:00~08:30 가능
            // 화요일: 비트맵 2 = 08:30~09:00 가능
            // 수~일: 비트맵 0 = 가용시간 없음
            Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
            bitmaps.put(DayOfWeek.MON, 1L);  // 월: 08:00~08:30
            bitmaps.put(DayOfWeek.TUE, 2L);  // 화: 08:30~09:00
            bitmaps.put(DayOfWeek.WED, 0L);  // 수: 없음
            bitmaps.put(DayOfWeek.THU, 0L);  // 목: 없음
            bitmaps.put(DayOfWeek.FRI, 0L);  // 금: 없음
            bitmaps.put(DayOfWeek.SAT, 0L);  // 토: 없음
            bitmaps.put(DayOfWeek.SUN, 0L);  // 일: 없음

            // when
            Map<DayOfWeek, boolean[]> result = AvailabilityBitmapConverter.toBooleanArrays(bitmaps);

            // then: 7개 요일 모두 변환됨
            assertThat(result).hasSize(7);
            assertThat(result.get(DayOfWeek.MON)[0]).isTrue();  // 월 08:00~08:30 가능
            assertThat(result.get(DayOfWeek.TUE)[1]).isTrue();  // 화 08:30~09:00 가능
        }

        @Test
        @DisplayName("누락된 요일은 0으로 처리")
        void missingDaysAsZero() {
            // given: 월요일만 설정하고 나머지 요일은 누락
            Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
            bitmaps.put(DayOfWeek.MON, 1L);  // 월요일만 08:00~08:30 가능

            // when
            Map<DayOfWeek, boolean[]> result = AvailabilityBitmapConverter.toBooleanArrays(bitmaps);

            // then: 누락된 요일은 자동으로 비트맵 0 (모두 false) 처리
            assertThat(result).hasSize(7);  // 7개 요일 모두 포함
            assertThat(result.get(DayOfWeek.MON)[0]).isTrue();      // 월: 설정대로
            assertThat(result.get(DayOfWeek.TUE)).containsOnly(false);  // 화: 자동 0 처리
        }
    }

    @Nested
    @DisplayName("createEmptyAvailability() - 빈 가용시간 생성")
    class CreateEmptyAvailabilityTest {

        @Test
        @DisplayName("모든 요일에 대해 false 배열 생성")
        void createEmptyForAllDays() {
            // when
            Map<DayOfWeek, boolean[]> result = AvailabilityBitmapConverter.createEmptyAvailability();

            // then
            assertThat(result).hasSize(7);
            for (DayOfWeek day : DayOfWeek.values()) {
                assertThat(result.get(day)).hasSize(32);
                assertThat(result.get(day)).containsOnly(false);
            }
        }
    }
}
