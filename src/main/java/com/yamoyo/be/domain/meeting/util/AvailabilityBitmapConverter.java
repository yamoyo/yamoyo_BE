package com.yamoyo.be.domain.meeting.util;

import com.yamoyo.be.domain.meeting.entity.WeeklyAvailability;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;

import java.util.EnumMap;
import java.util.Map;

public final class AvailabilityBitmapConverter {

    private AvailabilityBitmapConverter() {
    }

    public static Map<DayOfWeek, boolean[]> toBooleanArrays(Map<DayOfWeek, Long> bitmaps) {
        Map<DayOfWeek, boolean[]> result = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            Long bitmap = bitmaps.getOrDefault(day, 0L);
            result.put(day, toBooleanArray(bitmap));
        }
        return result;
    }

    public static Map<DayOfWeek, Long> toBitmaps(Map<DayOfWeek, boolean[]> booleanArrays) {
        Map<DayOfWeek, Long> result = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            boolean[] array = booleanArrays.getOrDefault(day, new boolean[WeeklyAvailability.SLOT_COUNT]);
            result.put(day, toBitmap(array));
        }
        return result;
    }

    public static boolean[] toBooleanArray(Long bitmap) {
        boolean[] result = new boolean[WeeklyAvailability.SLOT_COUNT];
        if (bitmap == null) {
            return result;
        }
        
        for (int i = 0; i < WeeklyAvailability.SLOT_COUNT; i++) {
            result[i] = (bitmap & (1L << i)) != 0;
        }
        
        return result;
    }


    public static Long toBitmap(boolean[] booleanArray) {
        if (booleanArray == null || booleanArray.length == 0) {
            return 0L;
        }
        long result = 0L;
        int length = Math.min(booleanArray.length, WeeklyAvailability.SLOT_COUNT);

        for (int i = 0; i < length; i++) {
            if (booleanArray[i]) {
                result |= (1L << i);
            }
        }
        return result;
    }

    public static Map<DayOfWeek, boolean[]> createEmptyAvailability() {
        Map<DayOfWeek, boolean[]> result = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            result.put(day, new boolean[WeeklyAvailability.SLOT_COUNT]);
        }
        return result;
    }
}
