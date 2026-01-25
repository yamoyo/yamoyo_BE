package com.yamoyo.be.domain.meeting.dto.request;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AvailabilitySubmitRequest(
        @NotNull(message = "가용시간 정보는 필수입니다.")
        AvailabilityData availability
) {
    public record AvailabilityData(
            @NotNull @Size(min = 32, max = 32) Boolean[] sunday,
            @NotNull @Size(min = 32, max = 32) Boolean[] monday,
            @NotNull @Size(min = 32, max = 32) Boolean[] tuesday,
            @NotNull @Size(min = 32, max = 32) Boolean[] wednesday,
            @NotNull @Size(min = 32, max = 32) Boolean[] thursday,
            @NotNull @Size(min = 32, max = 32) Boolean[] friday,
            @NotNull @Size(min = 32, max = 32) Boolean[] saturday
    ) {
        public Map<DayOfWeek, boolean[]> toDayOfWeekMap() {
            return Map.of(
                    DayOfWeek.SUN, toPrimitiveArray(sunday),
                    DayOfWeek.MON, toPrimitiveArray(monday),
                    DayOfWeek.TUE, toPrimitiveArray(tuesday),
                    DayOfWeek.WED, toPrimitiveArray(wednesday),
                    DayOfWeek.THU, toPrimitiveArray(thursday),
                    DayOfWeek.FRI, toPrimitiveArray(friday),
                    DayOfWeek.SAT, toPrimitiveArray(saturday)
            );
        }

        private boolean[] toPrimitiveArray(Boolean[] array) {
            boolean[] result = new boolean[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i] != null && array[i];
            }
            return result;
        }
    }
}
