-- V9: status 컬럼을 availability_status와 preferred_block_status로 분리

-- Step 1: 새 컬럼 추가
ALTER TABLE timepick_participants
    ADD COLUMN availability_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING / SUBMITTED / EXPIRED' AFTER preferred_block;

ALTER TABLE timepick_participants
    ADD COLUMN preferred_block_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING / SUBMITTED / EXPIRED' AFTER availability_status;

-- Step 2: 기존 데이터 마이그레이션
UPDATE timepick_participants
SET
    availability_status = status,
    preferred_block_status = status;

-- Step 3: 기존 status 컬럼 삭제
ALTER TABLE timepick_participants
    DROP COLUMN status;
