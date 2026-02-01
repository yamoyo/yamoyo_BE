package com.yamoyo.be.domain.meeting.everytime.util;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Everytime 시간표 데이터를 타임픽 슬롯 형식으로 변환하는 유틸리티.
 *
 * <h3>Everytime 시간 형식</h3>
 * <ul>
 *   <li>time값: 실제 분 = time × 5</li>
 *   <li>day: 0=월, 1=화, 2=수, 3=목, 4=금</li>
 * </ul>
 *
 * <h3>타임픽 슬롯 형식</h3>
 * <ul>
 *   <li>인덱스 0~31: 08:00~24:00 (30분 단위)</li>
 *   <li>true = 가능, false = 수업있음 (불가능)</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EverytimeTimeSlotConverter {

    private static final int SLOT_COUNT = 32;
    private static final int MINUTES_PER_SLOT = 30;
    private static final int EVERYTIME_TIME_UNIT = 5;
    private static final int START_HOUR_OFFSET_MINUTES = 480; // 08:00 = 480분

    /**
     * Everytime day 값을 DayOfWeek로 변환한다.
     *
     * @param day Everytime day 값 (0=월 ~ 4=금)
     * @return 대응하는 DayOfWeek
     */
    public static DayOfWeek toDayOfWeek(int day) {
        return switch (day) {
            case 0 -> DayOfWeek.MON;
            case 1 -> DayOfWeek.TUE;
            case 2 -> DayOfWeek.WED;
            case 3 -> DayOfWeek.THU;
            case 4 -> DayOfWeek.FRI;
            default -> throw new IllegalArgumentException("Invalid day: " + day);
        };
    }

    /**
     * Everytime time 값을 분으로 변환한다.
     *
     * @param everytimeTime Everytime time 값
     * @return 실제 분 (예: 96 → 480분 = 08:00)
     */
    public static int toMinutes(int everytimeTime) {
        return everytimeTime * EVERYTIME_TIME_UNIT;
    }

    /**
     * 분을 슬롯 인덱스로 변환한다.
     *
     * @param minutes 분 (480~1439, 08:00~23:59)
     * @return 슬롯 인덱스 (0~31, 범위 초과 시 -1)
     */
    public static int toSlotIndex(int minutes) {
        int adjustedMinutes = minutes - START_HOUR_OFFSET_MINUTES; // 08:00 기준으로 변환
        if (adjustedMinutes < 0) {
            return -1;
        }
        int index = adjustedMinutes / MINUTES_PER_SLOT;
        return (index < SLOT_COUNT) ? index : -1;
    }

    /**
     * 수업 시간 범위를 받아서 해당 슬롯들을 false로 마킹한다.
     *
     * @param slots 수정할 슬롯 배열 (길이 32)
     * @param startMinutes 시작 분
     * @param endMinutes 종료 분
     */
    public static void markBusy(boolean[] slots, int startMinutes, int endMinutes) {
        int startSlot = toSlotIndex(startMinutes);
        int endSlot = toSlotIndex(endMinutes - 1); // 종료 시간은 exclusive

        if (startSlot < 0) startSlot = 0;
        if (endSlot < 0 || endSlot >= SLOT_COUNT) endSlot = SLOT_COUNT - 1;

        for (int i = startSlot; i <= endSlot; i++) {
            slots[i] = false;
        }
    }

    /**
     * 전체 슬롯이 가능한 상태(true)로 초기화된 맵을 생성한다.
     * 토/일은 수업이 없으므로 전체 true로 유지된다.
     *
     * @return 요일별 boolean[32] 맵
     */
    public static Map<DayOfWeek, boolean[]> createFullAvailabilityMap() {
        Map<DayOfWeek, boolean[]> map = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean[] slots = new boolean[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) {
                slots[i] = true;
            }
            map.put(day, slots);
        }
        return map;
    }
}
