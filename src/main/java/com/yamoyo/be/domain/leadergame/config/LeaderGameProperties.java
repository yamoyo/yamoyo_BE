package com.yamoyo.be.domain.leadergame.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "leadergame")
public class LeaderGameProperties {

    /**
     * Volunteer(지원) 단계 지속 시간(초).
     * 운영에서는 환경변수 VOLUNTEER_DURATION_SECONDS로 오버라이드할 수 있다.
     */
    private long volunteerDurationSeconds = 10;
}

