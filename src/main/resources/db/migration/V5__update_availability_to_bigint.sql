-- V5: Update availability columns from INT UNSIGNED to BIGINT
-- JPA Entity expects Long type (BIGINT), but DB schema has INT UNSIGNED

-- timepick_participants 테이블
ALTER TABLE timepick_participants
    MODIFY availability_mon BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_tue BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_wed BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_thu BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_fri BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_sat BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_sun BIGINT NOT NULL DEFAULT 0;

-- user_timepick_defaults 테이블
ALTER TABLE user_timepick_defaults
    MODIFY availability_mon BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_tue BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_wed BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_thu BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_fri BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_sat BIGINT NOT NULL DEFAULT 0,
    MODIFY availability_sun BIGINT NOT NULL DEFAULT 0;
