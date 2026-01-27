package com.yamoyo.be.domain.meeting.entity.enums;

public enum DayOfWeek {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN;

    public static DayOfWeek from(java.time.DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> MON;
            case TUESDAY -> TUE;
            case WEDNESDAY -> WED;
            case THURSDAY -> THU;
            case FRIDAY -> FRI;
            case SATURDAY -> SAT;
            case SUNDAY -> SUN;
        };
    }

    public java.time.DayOfWeek toJavaDayOfWeek() {
        return switch (this) {
            case MON -> java.time.DayOfWeek.MONDAY;
            case TUE -> java.time.DayOfWeek.TUESDAY;
            case WED -> java.time.DayOfWeek.WEDNESDAY;
            case THU -> java.time.DayOfWeek.THURSDAY;
            case FRI -> java.time.DayOfWeek.FRIDAY;
            case SAT -> java.time.DayOfWeek.SATURDAY;
            case SUN -> java.time.DayOfWeek.SUNDAY;
        };
    }
}
