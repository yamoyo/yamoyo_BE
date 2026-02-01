package com.yamoyo.be.domain.meeting.everytime.strategy;

import com.yamoyo.be.domain.meeting.entity.enums.DayOfWeek;

import java.util.Map;

/**
 * Everytime 시간표 파싱 전략 인터페이스.
 *
 * <p>Strategy 패턴을 적용하여 다양한 파싱 방식을 지원한다.
 * 현재 API 방식이 기본 구현이며, 향후 브라우저 크롤링 방식 추가 가능.
 */
public interface EverytimeParsingStrategy {

    /**
     * Everytime 시간표를 파싱하여 가용시간 맵을 반환한다.
     *
     * @param identifier URL에서 추출한 사용자 식별자
     * @return 요일별 boolean[32] (true = 가능, false = 수업있음)
     */
    Map<DayOfWeek, boolean[]> parse(String identifier);
}
