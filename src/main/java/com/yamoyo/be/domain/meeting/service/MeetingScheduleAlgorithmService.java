package com.yamoyo.be.domain.meeting.service;

import com.yamoyo.be.domain.meeting.entity.TimepickParticipant;
import com.yamoyo.be.domain.meeting.entity.WeeklyAvailability;
import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [우선순위]
 * 1. 참석 가능 인원 최대화 (내림차순 → 큰 값이 우선)
 * 2. 선호 시간대 인원수 최대화 (내림차순 → 큰 값이 우선)
 * 3. 요일 우선순위: 월 > 화 > 수 > 목 > 금 > 토 > 일 (오름차순 → 작은 ordinal이 우선)
 * 4. 이른 시작 시각 (오름차순 → 작은 슬롯이 우선)
 */
@Service
@Slf4j
public class MeetingScheduleAlgorithmService {

    private static final LocalTime BASE_TIME = LocalTime.of(8, 0);

    public record SlotCandidate(
            DayOfWeek dayOfWeek,
            int startSlotIndex,
            int availableCount,
            int preferredCount
    ) {}

    public record ScheduleResult(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            int availableCount,
            int preferredCount
    ) {}

    /**
     * [미응답자 처리]
     * - 가용시간 미제출(PENDING/EXPIRED) → 전체 시간 가능으로 간주
     * - 선호시간대 미제출(PENDING/EXPIRED) → 20:00~24:00 선호로 간주
     */
    public ScheduleResult calculateOptimalSchedule(List<TimepickParticipant> participants) {
        List<SlotCandidate> candidates = generateAllCandidates(participants);

        SlotCandidate optimal = candidates.stream()
                .max(createPriorityComparator())
                .orElseThrow(() -> new IllegalStateException("슬롯 후보가 없습니다."));

        LocalTime startTime = convertSlotToTime(optimal.startSlotIndex());

        log.info("최적 시간 계산 완료 - 요일: {}, 시작시간: {}, 참석가능: {}명, 선호: {}명",
                optimal.dayOfWeek(), startTime, optimal.availableCount(), optimal.preferredCount());

        return new ScheduleResult(
                optimal.dayOfWeek(),
                startTime,
                optimal.availableCount(),
                optimal.preferredCount()
        );
    }

    /**
     * 모든 요일 x 슬롯 조합에 대한 후보를 생성한다.
     * 7요일 x 31슬롯(마지막 슬롯은 1시간 회의 불가) = 217개 후보
     */
    private List<SlotCandidate> generateAllCandidates(List<TimepickParticipant> participants) {
        List<SlotCandidate> candidates = new ArrayList<>();

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            for (int slotIndex = 0; slotIndex < WeeklyAvailability.SLOT_COUNT - 1; slotIndex++) {
                SlotCandidate candidate = evaluateSlot(dayOfWeek, slotIndex, participants);
                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private SlotCandidate evaluateSlot(DayOfWeek dayOfWeek, int startSlotIndex,
                                       List<TimepickParticipant> participants) {
        int availableCount = 0;
        int preferredCount = 0;

        for (TimepickParticipant participant : participants) {
            if (participant.canAttendOneHourMeetingAt(dayOfWeek, startSlotIndex)) {
                availableCount++;
                if (participant.prefersSlot(startSlotIndex)) {
                    preferredCount++;
                }
            }
        }

        return new SlotCandidate(dayOfWeek, startSlotIndex, availableCount, preferredCount);
    }

    private LocalTime convertSlotToTime(int slotIndex) {
        return BASE_TIME.plusMinutes(slotIndex * (long) WeeklyAvailability.SLOT_DURATION_MINUTES);
    }

    private Comparator<SlotCandidate> createPriorityComparator() {
        return Comparator
                // 1. 참석 가능 인원이 많을수록 우선 (내림차순)
                .comparingInt(SlotCandidate::availableCount)
                // 2. 선호 인원이 많을수록 우선 (내림차순)
                .thenComparingInt(SlotCandidate::preferredCount)
                // 3. 요일 ordinal이 작을수록 우선 (월요일이 0) → 음수로 변환해서 작은 값이 크게
                .thenComparingInt(candidate -> -candidate.dayOfWeek().ordinal())
                // 4. 이른 시작 시각 우선 → 음수로 변환해서 작은 값이 크게
                .thenComparingInt(candidate -> -candidate.startSlotIndex());
    }
}
