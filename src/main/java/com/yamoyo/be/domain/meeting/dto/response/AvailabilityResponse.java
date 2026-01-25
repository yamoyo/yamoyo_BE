package com.yamoyo.be.domain.meeting.dto.response;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;

import java.util.Map;

public record AvailabilityResponse(
    AvailabilityData availability
) {

    public record AvailabilityData(
            boolean[] sunday,
            boolean[] monday,
            boolean[] tuesday,
            boolean[] wednesday,
            boolean[] thursday,
            boolean[] friday,
            boolean[] saturday
    ) {
        public static AvailabilityData from(Map<DayOfWeek, boolean[]> availabilityMap) {
            return new AvailabilityData(
                    availabilityMap.get(DayOfWeek.SUN),
                    availabilityMap.get(DayOfWeek.MON),
                    availabilityMap.get(DayOfWeek.TUE),
                    availabilityMap.get(DayOfWeek.WED),
                    availabilityMap.get(DayOfWeek.THU),
                    availabilityMap.get(DayOfWeek.FRI),
                    availabilityMap.get(DayOfWeek.SAT)
            );
        }
    }

    public static AvailabilityResponse from(Map<DayOfWeek, boolean[]> availabilityMap) {
        return new AvailabilityResponse(AvailabilityData.from(availabilityMap));
    }
}
