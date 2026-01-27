package com.yamoyo.be.domain.meeting.entity;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyAvailability {

    @Column(name = "availability_mon", nullable = false)
    private Long availabilityMon = 0L;

    @Column(name = "availability_tue", nullable = false)
    private Long availabilityTue = 0L;

    @Column(name = "availability_wed", nullable = false)
    private Long availabilityWed = 0L;

    @Column(name = "availability_thu", nullable = false)
    private Long availabilityThu = 0L;

    @Column(name = "availability_fri", nullable = false)
    private Long availabilityFri = 0L;

    @Column(name = "availability_sat", nullable = false)
    private Long availabilitySat = 0L;

    @Column(name = "availability_sun", nullable = false)
    private Long availabilitySun = 0L;

    private WeeklyAvailability(Long availabilityMon, Long availabilityTue, Long availabilityWed,
                               Long availabilityThu, Long availabilityFri, Long availabilitySat,
                               Long availabilitySun) {
        this.availabilityMon = availabilityMon;
        this.availabilityTue = availabilityTue;
        this.availabilityWed = availabilityWed;
        this.availabilityThu = availabilityThu;
        this.availabilityFri = availabilityFri;
        this.availabilitySat = availabilitySat;
        this.availabilitySun = availabilitySun;
    }

    public static WeeklyAvailability empty() {
        return new WeeklyAvailability(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    public static WeeklyAvailability from(Map<DayOfWeek, Long> bitmaps) {
        return new WeeklyAvailability(
                bitmaps.getOrDefault(DayOfWeek.MON, 0L),
                bitmaps.getOrDefault(DayOfWeek.TUE, 0L),
                bitmaps.getOrDefault(DayOfWeek.WED, 0L),
                bitmaps.getOrDefault(DayOfWeek.THU, 0L),
                bitmaps.getOrDefault(DayOfWeek.FRI, 0L),
                bitmaps.getOrDefault(DayOfWeek.SAT, 0L),
                bitmaps.getOrDefault(DayOfWeek.SUN, 0L)
        );
    }

    public void update(Map<DayOfWeek, Long> bitmaps) {
        this.availabilityMon = bitmaps.getOrDefault(DayOfWeek.MON, 0L);
        this.availabilityTue = bitmaps.getOrDefault(DayOfWeek.TUE, 0L);
        this.availabilityWed = bitmaps.getOrDefault(DayOfWeek.WED, 0L);
        this.availabilityThu = bitmaps.getOrDefault(DayOfWeek.THU, 0L);
        this.availabilityFri = bitmaps.getOrDefault(DayOfWeek.FRI, 0L);
        this.availabilitySat = bitmaps.getOrDefault(DayOfWeek.SAT, 0L);
        this.availabilitySun = bitmaps.getOrDefault(DayOfWeek.SUN, 0L);
    }

    public Map<DayOfWeek, Long> toBitmaps() {
        Map<DayOfWeek, Long> bitmaps = new EnumMap<>(DayOfWeek.class);
        bitmaps.put(DayOfWeek.MON, availabilityMon);
        bitmaps.put(DayOfWeek.TUE, availabilityTue);
        bitmaps.put(DayOfWeek.WED, availabilityWed);
        bitmaps.put(DayOfWeek.THU, availabilityThu);
        bitmaps.put(DayOfWeek.FRI, availabilityFri);
        bitmaps.put(DayOfWeek.SAT, availabilitySat);
        bitmaps.put(DayOfWeek.SUN, availabilitySun);
        return bitmaps;
    }
}
